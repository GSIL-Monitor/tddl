package com.taobao.tddl.client.sequence.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.eagleeye.EagleEye;
import com.taobao.tddl.client.sequence.SequenceDao;
import com.taobao.tddl.client.sequence.SequenceRange;
import com.taobao.tddl.client.sequence.exception.SequenceException;
import com.taobao.tddl.client.sequence.util.RandomSequence;
//import com.taobao.tddl.client.util.DataSourceType;
import com.taobao.tddl.common.GroupDataSourceRouteHelper;
import com.taobao.tddl.jdbc.group.TGroupDataSource;

public class GroupSequenceDao implements SequenceDao {

	private static final Log log = LogFactory.getLog(GroupSequenceDao.class);

	// private static final int MIN_STEP = 1;
	// private static final int MAX_STEP = 100000;

	private static final int DEFAULT_INNER_STEP = 1000;

	private static final int DEFAULT_RETRY_TIMES = 2;

	private static final String DEFAULT_TABLE_NAME = "sequence";
	private static final String DEFAULT_TEMP_TABLE_NAME = "sequence_temp";
	
	private static final String DEFAULT_NAME_COLUMN_NAME = "name";
	private static final String DEFAULT_VALUE_COLUMN_NAME = "value";
	private static final String DEFAULT_GMT_MODIFIED_COLUMN_NAME = "gmt_modified";

	private static final int DEFAULT_DSCOUNT = 2;// Ĭ��
	private static final Boolean DEFAULT_ADJUST = false;

	protected static final long DELTA = 100000000L;
	// /**
	// * ����Դ����
	// */
	// private DataSourceMatrixCreator dataSourceMatrixCreator;

	/**
	 * Ӧ����
	 */
	protected String appName;

	/**
	 * group����
	 */
	protected List<String> dbGroupKeys;

	protected List<String> oriDbGroupKeys;

	/**
	 * groupDsʹ�õ�����Դ����
	 */
//	protected DataSourceType dataSourceType = DataSourceType.TbDataSource;

	/**
	 * ����Դ
	 */
	protected Map<String, DataSource> dataSourceMap;

	/**
	 * ����Ӧ����
	 */
	protected boolean adjust = DEFAULT_ADJUST;
	/**
	 * ���Դ���
	 */
	protected int retryTimes = DEFAULT_RETRY_TIMES;

	/**
	 * ����Դ����
	 */
	protected int dscount = DEFAULT_DSCOUNT;

	/**
	 * �ڲ���
	 */
	protected int innerStep = DEFAULT_INNER_STEP;

	/**
	 * �ⲽ��
	 */
	protected int outStep = DEFAULT_INNER_STEP;

	/**
	 * �������ڵı���
	 */
	protected String tableName = DEFAULT_TABLE_NAME;

	protected String switchTempTable = DEFAULT_TEMP_TABLE_NAME;
	
	private String TEST_TABLE_PREFIX="__test_";
	// ȫ��·ѹ���Ӧsequence���Ӱ�ӱ�
	protected String testTableName = TEST_TABLE_PREFIX + tableName;
	// ȫ��·ѹ���Ӧsequence_temp���Ӱ�ӱ�
	protected String testSwitchTempTable = TEST_TABLE_PREFIX + switchTempTable;

	/**
	 * �洢�������Ƶ�����
	 */
	protected String nameColumnName = DEFAULT_NAME_COLUMN_NAME;

	/**
	 * �洢����ֵ������
	 */
	protected String valueColumnName = DEFAULT_VALUE_COLUMN_NAME;

	/**
	 * �洢����������ʱ�������
	 */
	protected String gmtModifiedColumnName = DEFAULT_GMT_MODIFIED_COLUMN_NAME;

	/**
	 * ���Ի�
	 * 
	 * @throws SequenceException
	 */
	public void init() throws SequenceException {
		// ���Ӧ����Ϊ�գ�ֱ���׳�
		if (StringUtils.isEmpty(appName)) {
			SequenceException sequenceException = new SequenceException(
					"appName is Null ");
			log.error("û������appName", sequenceException);
			throw sequenceException;
		}
		if (dbGroupKeys == null || dbGroupKeys.size() == 0) {
			log.error("û������dbgroupKeys");
			throw new SequenceException("dbgroupKeysΪ�գ�");
		}

		dataSourceMap = new HashMap<String, DataSource>();
		for (String dbGroupKey : dbGroupKeys) {
			if (dbGroupKey.toUpperCase().endsWith("-OFF")) {
				continue;
			}
//			TGroupDataSource tGroupDataSource = new TGroupDataSource(
//					dbGroupKey, appName, dataSourceType);
			TGroupDataSource tGroupDataSource = new TGroupDataSource(
					dbGroupKey, appName);
			tGroupDataSource.init();
			dataSourceMap.put(dbGroupKey, tGroupDataSource);
		}
		if (dbGroupKeys.size() >= dscount) {
			dscount = dbGroupKeys.size();
		} else {
			for (int ii = dbGroupKeys.size(); ii < dscount; ii++) {
				dbGroupKeys.add(dscount + "-OFF");
			}
		}
		outStep = innerStep * dscount;// �����ⲽ��

		StringBuilder sb = new StringBuilder();
		sb.append("GroupSequenceDao��ʼ����ɣ�\r\n ");
		sb.append("appName:").append(appName).append("\r\n");
		sb.append("innerStep:").append(this.innerStep).append("\r\n");
		sb.append("dataSource:").append(dscount).append("��:");
		for (String str : dbGroupKeys) {
			sb.append("[").append(str).append("]��");
		}
		sb.append("\r\n");
		sb.append("adjust��").append(adjust).append("\r\n");
		sb.append("retryTimes:").append(retryTimes).append("\r\n");
		sb.append("tableName:").append(tableName).append("\r\n");
		sb.append("nameColumnName:").append(nameColumnName).append("\r\n");
		sb.append("valueColumnName:").append(valueColumnName).append("\r\n");
		sb.append("gmtModifiedColumnName:").append(gmtModifiedColumnName)
				.append("\r\n");
		log.info(sb.toString());
	}

	/**
	 * 
	 * @param index
	 *            gourp�ڵ���ţ���0��ʼ
	 * @param value
	 *            ��ǰȡ��ֵ
	 * @return
	 */
	private boolean check(int index, long value) {
		return (value % outStep) == (index * innerStep);
	}

	/**
	 * ��鲢����ĳ��sequence 1�����sequece�����ڣ�����ֵ������ʼ��ֵ 2������Ѿ����ڣ������ص�����������
	 * 3������Ѿ����ڣ������ص���
	 * 
	 * @throws SequenceException
	 */
	public void adjust(String name) throws SequenceException, SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		for (int i = 0; i < dbGroupKeys.size(); i++) {
			if (dbGroupKeys.get(i).toUpperCase().endsWith("-OFF"))// �Ѿ��ص���������
			{
				continue;
			}
			TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
					.get(dbGroupKeys.get(i));
			try {
				conn = tGroupDataSource.getConnection();
				stmt = conn.prepareStatement(getSelectSql());
				stmt.setString(1, name);
				GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
				rs = stmt.executeQuery();
				int item = 0;
				while (rs.next()) {
					item++;
					long val = rs.getLong(this.getValueColumnName());
					if (!check(i, val)) // �����ֵ
					{
						if (this.isAdjust()) {
							this.adjustUpdate(i, val, name);
						} else {
							log.error("���ݿ������õĳ�ֵ���������������ݿ⣬��������adjust����");
							throw new SequenceException(
									"���ݿ������õĳ�ֵ���������������ݿ⣬��������adjust����");
						}
					}
				}
				if (item == 0)// ������,����������¼
				{
					if (this.isAdjust()) {
						this.adjustInsert(i, name);
					} else {
						log.error("���ݿ���δ���ø�sequence���������ݿ��в���sequence��¼����������adjust����");
						throw new SequenceException(
								"���ݿ���δ���ø�sequence���������ݿ��в���sequence��¼����������adjust����");
					}
				}
			} catch (SQLException e) {// �̵�SQL�쳣�������������õĿ����
				log.error("��ֵУ�������Ӧ�����г���.", e);
				throw e;
			} finally {
				closeResultSet(rs);
				rs = null;
				closeStatement(stmt);
				stmt = null;
				closeConnection(conn);
				conn = null;

			}

		}
	}

	/**
	 * ����
	 * 
	 * @param index
	 * @param value
	 * @param name
	 * @throws SequenceException
	 * @throws SQLException
	 */
	private void adjustUpdate(int index, long value, String name)
			throws SequenceException, SQLException {
		long newValue = (value - value % outStep) + outStep + index * innerStep;// ���ó��µĵ���ֵ
		TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
				.get(dbGroupKeys.get(index));
		Connection conn = null;
		PreparedStatement stmt = null;
		// ResultSet rs = null;
		try {
			conn = tGroupDataSource.getConnection();
			stmt = conn.prepareStatement(getUpdateSql());
			stmt.setLong(1, newValue);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setString(3, name);
			stmt.setLong(4, value);
			GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
			int affectedRows = stmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SequenceException(
						"faild to auto adjust init value at  " + name
								+ " update affectedRow =0");
			}
			log.info(dbGroupKeys.get(index) + "���³�ֵ�ɹ�!" + "sequence Name��"
					+ name + "���¹��̣�" + value + "-->" + newValue);
		} catch (SQLException e) { // �Ե�SQL�쳣����Sequence�쳣
			log.error(
					"����SQLException,���³�ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "���¹��̣�" + value + "-->" + newValue, e);
			throw new SequenceException(
					"����SQLException,���³�ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "���¹��̣�" + value + "-->" + newValue, e);
		} finally {
			closeStatement(stmt);
			stmt = null;
			closeConnection(conn);
			conn = null;
		}
	}

	/**
	 * ������ֵ
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws SequenceException
	 * @throws SQLException
	 */
	private void adjustInsert(int index, String name) throws SequenceException,
			SQLException {
		TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
				.get(dbGroupKeys.get(index));
		long newValue = index * innerStep;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = tGroupDataSource.getConnection();
			stmt = conn.prepareStatement(getInsertSql());
			stmt.setString(1, name);
			stmt.setLong(2, newValue);
			stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
			int affectedRows = stmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SequenceException(
						"faild to auto adjust init value at  " + name
								+ " update affectedRow =0");
			}
			log.info(dbGroupKeys.get(index) + "   name:" + name + "�����ֵ:"
					+ name + "value:" + newValue);

		} catch (SQLException e) { // �Ե�SQL�쳣����sequence�쳣
			log.error(
					"����SQLException,�����ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "   value:" + newValue, e);
			throw new SequenceException(
					"����SQLException,�����ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "   value:" + newValue, e);
		} finally {
			closeResultSet(rs);
			rs = null;
			closeStatement(stmt);
			stmt = null;
			closeConnection(conn);
			conn = null;
		}
	}

	private ConcurrentHashMap<Integer/* ds index */, AtomicInteger/* �ӹ����� */> excludedKeyCount = new ConcurrentHashMap<Integer, AtomicInteger>(
			dscount);
	// ����Թ�������ָ�
	private int maxSkipCount = 10;
	// ʹ���������ݿⱣ��
	private boolean useSlowProtect = false;
	// ������ʱ��
	private int protectMilliseconds = 50;

	private ExecutorService exec = Executors.newFixedThreadPool(1);

	protected Lock configLock = new ReentrantLock();
	
	/**
	 * ���groupKey�����Ƿ��Ѿ��ر�
	 * @param groupKey
	 * @return
	 */
	protected boolean isOffState(String groupKey){
		return groupKey.toUpperCase().endsWith("-OFF");
	}
	
	/**
	 * ����Ƿ�exclude,����г��Իָ�
	 * @param index
	 * @return
	 */
	protected boolean recoverFromExcludes(int index) {
		boolean result = true;
		if (excludedKeyCount.get(index) != null) {
			if (excludedKeyCount.get(index).incrementAndGet() > maxSkipCount) {
				excludedKeyCount.remove(index);
				log.error(maxSkipCount + "�����ѹ���indexΪ" + index
						+ "������Դ�������³���ȡ����");
			} else {
				result = false;
			}
		}
		return result;
	}
	
	protected long queryOldValue(DataSource dataSource, String keyName) throws SQLException{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(getSelectSql());
			stmt.setString(1, keyName);
			GroupDataSourceRouteHelper
					.executeByGroupDataSourceIndex(0);
			rs = stmt.executeQuery();
			rs.next();
			return rs.getLong(1);
		} finally {
			// ֱ���׳��쳣����ӣ�����������Ҫֱ�ӹر�����
			closeDbResource(rs, stmt, conn);
		}
	}
	
	/**
	 * CAS����sequenceֵ
	 * @param dataSource
	 * @param keyName
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws SQLException
	 */
	protected int updateNewValue(DataSource dataSource, String keyName, long oldValue, long newValue) throws SQLException{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(getUpdateSql());
			stmt.setLong(1, newValue);
			stmt.setTimestamp(2,
					new Timestamp(System.currentTimeMillis()));
			stmt.setString(3, keyName);
			stmt.setLong(4, oldValue);
			GroupDataSourceRouteHelper
					.executeByGroupDataSourceIndex(0);
			return stmt.executeUpdate();
		} finally {
			// ֱ���׳��쳣����ӣ�����������Ҫֱ�ӹر�����
			closeDbResource(rs, stmt, conn);
		}
	}
	
	/**
	 * ��ָ�������ݿ��л�ȡsequenceֵ
	 * @param dataSource
	 * @param keyName
	 * @return
	 * @throws SQLException
	 */
	protected long getOldValue(final DataSource dataSource, final String keyName)  throws SQLException{
		long result = 0;
		
		// ���δʹ�ó�ʱ���������Ѿ�ֻʣ����1������Դ��������ô��ȥ��
		if (!useSlowProtect
				|| excludedKeyCount.size() >= (dscount - 1)) {
			result = queryOldValue(dataSource, keyName);
		} else {
			FutureTask<Long> future = new FutureTask<Long>(
					new Callable<Long>() {
						@Override
						public Long call() throws Exception {
							return queryOldValue(dataSource, keyName);
						}
					});
			try {
				exec.submit(future);
				result = future.get(protectMilliseconds,
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new SQLException(
						"[SEQUENCE SLOW-PROTECTED MODE]:InterruptedException",
						e);
			} catch (ExecutionException e) {
				throw new SQLException(
						"[SEQUENCE SLOW-PROTECTED MODE]:ExecutionException",
						e);
			} catch (TimeoutException e) {
				throw new SQLException(
						"[SEQUENCE SLOW-PROTECTED MODE]:TimeoutException,��ǰ���ó�ʱʱ��Ϊ"
								+ protectMilliseconds, e);
			}
		}
		return result;
	}
	
	/**
	 * ����oldValue����newValue
	 * @param index
	 * @param oldValue
	 * @param keyName
	 * @return
	 * @throws SequenceException
	 */
	protected long generateNewValue(int index, long oldValue, String keyName) throws SequenceException {
		long newValue = oldValue + outStep;
		if (!check(index, newValue)) // ���������ֵ������
		{
			if (this.isAdjust()) {
				newValue = adjustNewValue(index, newValue);
			} else {
				throwErrorRangeException(index, keyName);
			}
		}
		return newValue;
	}
	
	protected long adjustNewValue(int index, long newValue){
		return (newValue - newValue % outStep)
				+ outStep + index * innerStep;// ���ó��µĵ���ֵ
	}
	
	protected void throwErrorRangeException(int index, String keyName)
			throws SequenceException {
		String errorMsg = dbGroupKeys.get(index) + ":" + keyName
				+ "��ֵ�ô��󣬸��ǵ�������Χ���ˣ����޸����ݿ⣬���߿���adjust���أ�";
		throw new SequenceException(errorMsg);
	}
	
	
	protected TGroupDataSource getGroupDsByIndex(int index){
		return (TGroupDataSource) dataSourceMap.get(dbGroupKeys.get(index));
	}
	
	/**
	 * ����sequenceֵ�Ƿ���������Χ��
	 * @return
	 */
	protected boolean isOldValueFixed(long oldValue){
		boolean result = true;
		StringBuilder message = new StringBuilder();
		if (oldValue < 0) {
			message.append(
					"Sequence value cannot be less than zero.");
			result = false;
		}
		else if (oldValue > Long.MAX_VALUE - DELTA) {
			message.append("Sequence value overflow.");
			result = false;
		}
		if(!result){
			message.append(" Sequence value  = ").append(oldValue);
			message.append(", please check table ").append(getTableName());
			log.info(message);
		}
		return result;
	}
	
	/**
	 * ��������Դ�ų���sequence��ѡ����Դ����
	 * @param index
	 */
	protected void excludeDataSource(int index) {
		// �������Դֻʣ�������һ�����Ͳ�Ҫ�ų���
		if (excludedKeyCount.size() < (dscount - 1)) {
			excludedKeyCount.put(index, new AtomicInteger(0));
			log.error("��ʱ�߳�indexΪ" + index + "������Դ��" + maxSkipCount + "�κ����³���");
		}
	}

	public SequenceRange nextRange(final String name) throws SequenceException {
		if (name == null) {
			log.error("������Ϊ�գ�");
			throw new IllegalArgumentException("�������Ʋ���Ϊ��");
		}

		configLock.lock();
		try {
			int[] randomIntSequence = RandomSequence.randomIntSequence(dscount);
			for (int i = 0; i < retryTimes; i++) {
				for (int j = 0; j < dscount; j++) {
					int index = randomIntSequence[j];
					if (isOffState(dbGroupKeys.get(index)) || !recoverFromExcludes(index)) {
						continue;
					}

					final TGroupDataSource tGroupDataSource = getGroupDsByIndex(index);
					long oldValue;
					// ��ѯ��ֻ�����������ݿ�ҵ��������������ݿⱣ��
					try {
						oldValue = getOldValue(tGroupDataSource, name);
						if (!isOldValueFixed(oldValue)) {
							continue;
						}
					} catch (SQLException e) {
						log.error("ȡ��Χ������--��ѯ����" + dbGroupKeys.get(index)
								+ ":" + name, e);
						excludeDataSource(index);
						continue;
					}

					long newValue = generateNewValue(index, oldValue, name);
					try {
						if (0 == updateNewValue(tGroupDataSource, name, oldValue, newValue)) {
							continue;
						}
					} catch (SQLException e) {
						log.error("ȡ��Χ������--���³���" + dbGroupKeys.get(index)
								+ ":" + name, e);
						continue;
					}
					
					return new SequenceRange(newValue + 1, newValue
							+ innerStep);
						
				}
				// ���������һ�����Ի���ʱ,���excludedMap,���������һ�λ���
				if (i == (retryTimes - 2)) {
					excludedKeyCount.clear();
				}
			}
			log.error("��������Դ�������ã�������" + this.retryTimes + "�κ���Ȼʧ��!");
			throw new SequenceException("All dataSource faild to get value!");
		} finally {
			configLock.unlock();
		}
	}

	public void setDscount(int dscount) {
		this.dscount = dscount;
	}

	protected String getInsertSql() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("insert into ").append(getTableName()).append("(");
		buffer.append(getNameColumnName()).append(",");
		buffer.append(getValueColumnName()).append(",");
		buffer.append(getGmtModifiedColumnName()).append(") values(?,?,?);");
		return buffer.toString();
	}

	protected String getSelectSql() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ").append(getValueColumnName());
		buffer.append(" from ").append(getTableName());
		buffer.append(" where ").append(getNameColumnName()).append(" = ?");
		return buffer.toString();
	}

	protected String getUpdateSql() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("update ").append(getTableName());
		buffer.append(" set ").append(getValueColumnName()).append(" = ?, ");
		buffer.append(getGmtModifiedColumnName()).append(" = ? where ");
		buffer.append(getNameColumnName()).append(" = ? and ");
		buffer.append(getValueColumnName()).append(" = ?");
		return buffer.toString();
	}
	
	protected static void closeDbResource(ResultSet rs, Statement stmt, Connection conn){
		closeResultSet(rs);
		closeStatement(stmt);
		closeConnection(conn);
	}

	protected static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC ResultSet", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC ResultSet", e);
			}
		}
	}

	protected static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC Statement", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC Statement", e);
			}
		}
	}

	protected static void closeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC Connection", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC Connection", e);
			}
		}
	}

	public int getRetryTimes() {
		return retryTimes;
	}

	public void setRetryTimes(int retryTimes) {
		this.retryTimes = retryTimes;
	}

	public int getInnerStep() {
		return innerStep;
	}

	public void setInnerStep(int innerStep) {
		this.innerStep = innerStep;
	}

	public String getTableName() {
		// ȫ��·ѹ������
		String t = EagleEye.getUserData("t");
		if (!StringUtils.isBlank(t) && t.equals("1")) {
			return testTableName;
		} else {
			return tableName;
		}
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		this.testTableName = TEST_TABLE_PREFIX + this.tableName;
	}

	public String getNameColumnName() {
		return nameColumnName;
	}

	public void setNameColumnName(String nameColumnName) {
		this.nameColumnName = nameColumnName;
	}

	public String getValueColumnName() {
		return valueColumnName;
	}

	public void setValueColumnName(String valueColumnName) {
		this.valueColumnName = valueColumnName;
	}

	public String getGmtModifiedColumnName() {
		return gmtModifiedColumnName;
	}

	public void setGmtModifiedColumnName(String gmtModifiedColumnName) {
		this.gmtModifiedColumnName = gmtModifiedColumnName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setDbGroupKeys(List<String> dbGroupKeys) {
		//����ugly,�����̬���Ҳ������������Ļ����Ǿͼ�����
		this.oriDbGroupKeys = dbGroupKeys;
		this.dbGroupKeys = dbGroupKeys;
	}

//	public void setDataSourceType(DataSourceType dataSourceType) {
//		this.dataSourceType = dataSourceType;
//	}

	public boolean isAdjust() {
		return adjust;
	}

	public void setAdjust(boolean adjust) {
		this.adjust = adjust;
	}

	public int getMaxSkipCount() {
		return maxSkipCount;
	}

	public void setMaxSkipCount(int maxSkipCount) {
		this.maxSkipCount = maxSkipCount;
	}

	public boolean isUseSlowProtect() {
		return useSlowProtect;
	}

	public void setUseSlowProtect(boolean useSlowProtect) {
		this.useSlowProtect = useSlowProtect;
	}

	public int getProtectMilliseconds() {
		return protectMilliseconds;
	}

	public void setProtectMilliseconds(int protectMilliseconds) {
		this.protectMilliseconds = protectMilliseconds;
	}

	public String getSwitchTempTable() {
		String t = EagleEye.getUserData("t");
		if (!StringUtils.isBlank(t) && t.equals("1")) {
			return testSwitchTempTable;
		} else {
			return switchTempTable;
		}
	}

	public void setSwitchTempTable(String switchTempTable) {
		this.switchTempTable = switchTempTable;
		this.testSwitchTempTable = TEST_TABLE_PREFIX + this.switchTempTable;
	}
}

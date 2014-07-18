package com.taobao.tddl.jdbc.druid;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.config.ConfigDataListener;
import com.taobao.tddl.common.standard.atom.AtomDbStatusEnum;
import com.taobao.tddl.common.standard.atom.AtomDbTypeEnum;
import com.taobao.tddl.jdbc.druid.common.DruidConURLTools;
import com.taobao.tddl.jdbc.druid.common.DruidConfParser;
import com.taobao.tddl.jdbc.druid.common.DruidConstants;
import com.taobao.tddl.jdbc.druid.config.DbConfManager;
import com.taobao.tddl.jdbc.druid.config.DbPasswdManager;
import com.taobao.tddl.jdbc.druid.config.DiamondDbConfManager;
import com.taobao.tddl.jdbc.druid.config.DiamondDbPasswdManager;
import com.taobao.tddl.jdbc.druid.config.object.DruidDsConfDO;
import com.taobao.tddl.jdbc.druid.exception.DruidAlreadyInitException;
import com.taobao.tddl.jdbc.druid.exception.DruidIllegalException;
import com.taobao.tddl.jdbc.druid.exception.DruidInitialException;
import com.taobao.tddl.jdbc.druid.jdbc.TDataSourceWrapper;
import com.taobao.tddl.jdbc.druid.listener.DruidDbStatusListener;

/**
 * ���ݿ⶯̬�л���Handle�࣬�������ݿ�Ķ�̬�л� ��������������
 * 
 * @author qihao
 * 
 */
class DruidDsConfHandle {
	private static Log logger = LogFactory.getLog(DruidDsConfHandle.class);

	private String appName;

	private String dbKey;
	
	private String unitName;

	/**
	 * ����ʱ����
	 */
	private volatile DruidDsConfDO runTimeConf = new DruidDsConfDO();

	/**
	 * �������ã����������͵Ķ�̬����
	 */
	private DruidDsConfDO localConf = new DruidDsConfDO();

	/**
	 * ȫ�����ã�Ӧ�����ö��Ĺ���
	 */
	private DbConfManager dbConfManager;

	/**
	 * �������ö��Ĺ���
	 */
	private DbPasswdManager dbPasswdManager;

	/**
	 * druid����Դͨ��init��ʼ��
	 */
	private volatile DruidDataSource druidDataSource;

	/**
	 * ���ݿ�״̬�ı�ص�
	 */
	private volatile List<DruidDbStatusListener> dbStatusListeners;

	/**
	 * ��ʼ�����Ϊһ����ʼ���������б��ص����ý�ֹ�Ķ�
	 */
	private volatile boolean initFalg;

	/**
	 * ����Դ������������Ҫ������Դ�����ؽ�����ˢ��ʱ��Ҫ�Ȼ�ø���
	 */
	private final ReentrantLock lock = new ReentrantLock();

//	public static final int druidStatMaxKeySize = 5000;
//	public static final int druidFlushIntervalMill = 300*1000;
	
	/**
	 * ��ʼ��������������Ӧ������Դ��ֻ�ܱ�����һ��
	 * 
	 * @throws Exception
	 */
	protected void init() throws Exception {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] double call Init !");
		}
		// 1.��ʼ���������
		if (StringUtil.isBlank(this.appName) || StringUtil.isBlank(this.dbKey)) {
			String errorMsg = "[attributeError] TAtomDatasource of appName Or dbKey is Empty !";
			logger.error(errorMsg);
			throw new DruidIllegalException(errorMsg);
		}
		// 2.����dbConfManager
		DiamondDbConfManager defaultDbConfManager = new DiamondDbConfManager();
		defaultDbConfManager.setGlobalConfigDataId(DruidConstants
				.getGlobalDataId(this.dbKey));
		defaultDbConfManager.setAppConfigDataId(DruidConstants.getAppDataId(
				this.appName, this.dbKey));
		defaultDbConfManager.setUnitName(unitName);
		// ��ʼ��dbConfManager
		defaultDbConfManager.init(appName);
		dbConfManager = defaultDbConfManager;
		// 3.��ȡȫ������
		String globaConfStr = dbConfManager.getGlobalDbConf();
		// ע��ȫ�����ü���
		registerGlobaDbConfListener(defaultDbConfManager);
		if (StringUtil.isBlank(globaConfStr)) {
			String errorMsg = "[ConfError] read globalConfig is Empty !";
			logger.error(errorMsg);
			throw new DruidInitialException(errorMsg);
		}
		// 4.��ȡӦ������
		String appConfStr = dbConfManager.getAppDbDbConf();
		// ע��Ӧ�����ü���
		registerAppDbConfListener(defaultDbConfManager);
		if (StringUtil.isBlank(appConfStr)) {
			String errorMsg = "[ConfError] read appConfig is Empty !";
			logger.error(errorMsg);
			throw new DruidInitialException(errorMsg);
		}
		lock.lock();
		try {
			// 5.��������string��TAtomDsConfDO
			runTimeConf = DruidConfParser.parserTAtomDsConfDO(globaConfStr,
					appConfStr);
			// 6.��������������
			overConfByLocal(localConf, runTimeConf);
			// 7.���û�����ñ������룬���ö������룬��ʼ��passwdManager
			if (StringUtil.isBlank(this.runTimeConf.getPasswd())) {
				// ���dbKey�Ͷ�Ӧ��userName�Ƿ�Ϊ��
				if (StringUtil.isBlank(runTimeConf.getUserName())) {
					String errorMsg = "[attributeError] TAtomDatasource of UserName is Empty !";
					logger.error(errorMsg);
					throw new DruidIllegalException(errorMsg);
				}
				DiamondDbPasswdManager diamondDbPasswdManager = new DiamondDbPasswdManager();
				diamondDbPasswdManager.setPasswdConfDataId(DruidConstants
						.getPasswdDataId(runTimeConf.getDbName(),
								runTimeConf.getDbType(),
								runTimeConf.getUserName()));
				diamondDbPasswdManager.setUnitName(unitName);
				diamondDbPasswdManager.init(appName);
				dbPasswdManager = diamondDbPasswdManager;
				// ��ȡ����
				String passwd = dbPasswdManager.getPasswd();
				registerPasswdConfListener(diamondDbPasswdManager);
				if (StringUtil.isBlank(passwd)) {
					String errorMsg = "[PasswdError] read passwd is Empty !";
					logger.error(errorMsg);
					throw new DruidInitialException(errorMsg);
				}
				runTimeConf.setPasswd(passwd);
			}
			// 8.ת��tAtomDsConfDO
			DruidDataSource druidDataSource = convertTAtomDsConf2DruidConf(DruidDsConfHandle.this.dbKey,
					this.runTimeConf,
					DruidConstants.getDbNameStr(this.unitName, this.appName, this.dbKey));
			// 9.������������������ȷֱ���׳��쳣
			if (!checkLocalTxDataSourceDO(druidDataSource)) {
				String errorMsg = "[ConfigError]init dataSource Prams Error! config is : "
						+ druidDataSource.toString();
				logger.error(errorMsg);
				throw new DruidInitialException(errorMsg);
			}
//			 10.��������Դ
//			druidDataSource.setUseJmx(false);
//			LocalTxDataSource localTxDataSource = TaobaoDataSourceFactory
//					.createLocalTxDataSource(localTxDataSourceDO);
			//11.�������õ�����Դ��ָ��TAtomDatasource��
			druidDataSource.init();
			
//			druidDataSource.getDataSourceStat().setMaxSqlSize(DruidDsConfHandle.druidStatMaxKeySize);
			this.druidDataSource = druidDataSource;
			clearDataSourceWrapper();
			initFalg = true;
		} finally {
			lock.unlock();
		}
	}

	private void clearDataSourceWrapper() {
		Monitor.removeSnapshotValuesCallback(wrapDataSource);
		wrapDataSource = null;
	}

	/**
	 * ע������仯������
	 * 
	 * @param dbPasswdManager
	 */
	private void registerPasswdConfListener(DbPasswdManager dbPasswdManager) {
		dbPasswdManager.registerPasswdConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[Passwd HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String localPasswd = DruidDsConfHandle.this.localConf
							.getPasswd();
					if (StringUtil.isNotBlank(localPasswd)) {
						// �������������passwdֱ�ӷ��ز�֧�ֶ�̬�޸�
						return;
					}
					String newPasswd = DruidConfParser.parserPasswd(data);
					String runPasswd = DruidDsConfHandle.this.runTimeConf
							.getPasswd();
					if (!StringUtil.equals(runPasswd, newPasswd)) {
						try {
							//modify by junyu 2013-06-14:dynamic change passwd,not recreate it!
//							DruidDataSource newDruidDataSource = DruidDsConfHandle.this.druidDataSource.cloneDruidDataSource();
//							newDruidDataSource.setPassword(newPasswd);
//							newDruidDataSource.init();
//							DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
//							DruidDsConfHandle.this.druidDataSource = newDruidDataSource;
//							tempDataSource.close();
//							logger.warn("[DRUID CHANGE PASSWORD] ReCreate DataSource !");
							// �����µ����ø�������ʱ������
//							clearDataSourceWrapper();
							DruidDsConfHandle.this.druidDataSource.setPassword(newPasswd);
							logger.warn("[DRUID CHANGE PASSWORD] already reset the new passwd!");
							DruidDsConfHandle.this.runTimeConf
									.setPasswd(newPasswd);
						} catch (Exception e) {
							logger.error(
									"[DRUID CHANGE PASSWORD] reset new passwd error!",
									e);
						}
					}
				} finally {
					lock.unlock();
				}
			}
		});
	}

	/**
	 * ȫ�����ü���,ȫ�����÷����仯�� ��Ҫ����FLUSH����Դ
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerGlobaDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerGlobaDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[DRUID GlobaConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String globaConfStr = data;
					// �����ȫ�����÷����仯��������IP,PORT,DBNAME,DBTYPE,STATUS
					DruidDsConfDO tmpConf = DruidConfParser
							.parserTAtomDsConfDO(globaConfStr, null);
					DruidDsConfDO newConf = DruidDsConfHandle.this.runTimeConf
							.clone();
					// �������͵����ã����ǵ�ǰ������
					newConf.setIp(tmpConf.getIp());
					newConf.setPort(tmpConf.getPort());
					newConf.setDbName(tmpConf.getDbName());
					newConf.setDbType(tmpConf.getDbType());
					newConf.setDbStatus(tmpConf.getDbStatus());
					// ��������������
					overConfByLocal(DruidDsConfHandle.this.localConf, newConf);
					// ������͹��������ݿ�״̬�� RW/R->NA,ֱ�����ٵ�����Դ������ҵ���߼���������
					if (AtomDbStatusEnum.NA_STATUS != DruidDsConfHandle.this.runTimeConf
							.getDbStautsEnum()
							&& AtomDbStatusEnum.NA_STATUS == tmpConf
									.getDbStautsEnum()) {
						try {
							DruidDsConfHandle.this.druidDataSource.close();
							logger.warn("[DRUID NA STATUS PUSH] destroy DataSource !");
						} catch (Exception e) {
							logger.error(
									"[DRUID NA STATUS PUSH] destroy DataSource  Error!",
									e);
						}
					} else {
						// ת��tAtomDsConfDO
						DruidDataSource druidDataSource;
						try {
							druidDataSource = convertTAtomDsConf2DruidConf(DruidDsConfHandle.this.dbKey,
									newConf, DruidConstants.getDbNameStr(
											DruidDsConfHandle.this.unitName,
											DruidDsConfHandle.this.appName,
											DruidDsConfHandle.this.dbKey));
						} catch (Exception e1) {
							logger.error("[DRUID GlobaConfError] convertTAtomDsConf2DruidConf Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ���ת�������Ƿ���ȷ
						if (!checkLocalTxDataSourceDO(druidDataSource)) {
							logger.error("[DRUID GlobaConfError] dataSource Prams Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ������͵�״̬ʱ NA->RW/R ʱ��Ҫ���´�������Դ��������ˢ��
						if (DruidDsConfHandle.this.runTimeConf
								.getDbStautsEnum() == AtomDbStatusEnum.NA_STATUS
								&& (newConf.getDbStautsEnum() == AtomDbStatusEnum.RW_STATUS
										|| newConf.getDbStautsEnum() == AtomDbStatusEnum.R_STATUS || newConf
										.getDbStautsEnum() == AtomDbStatusEnum.W_STATUS)) {
							// ��������Դ
							try {
								// �ر�TB-DATASOURCE��JMXע��
								// localTxDataSourceDO.setUseJmx(false);
								// LocalTxDataSource localTxDataSource =
								// TaobaoDataSourceFactory
								// .createLocalTxDataSource(localTxDataSourceDO);
								druidDataSource.init();
//								druidDataSource.getDataSourceStat().setMaxSqlSize(DruidDsConfHandle.druidStatMaxKeySize);
								DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
								DruidDsConfHandle.this.druidDataSource = druidDataSource;
								tempDataSource.close();
								logger.warn("[DRUID NA->RW/R STATUS PUSH] ReCreate DataSource !");
							} catch (Exception e) {
								logger.error(
										"[DRUID NA->RW/R STATUS PUSH] ReCreate DataSource Error!",
										e);
							}
						} else {
							boolean needCreate = isGlobalChangeNeedReCreate(
									DruidDsConfHandle.this.runTimeConf, newConf);
							// ������������ñ仯�Ƿ���Ҫ�ؽ�����Դ
							// druid û��flush������ֻ���ؽ�����Դ jiechen.qzm
							if (needCreate) {
								try {
									// ��������Դ
									druidDataSource.init();
//									druidDataSource.getDataSourceStat().setMaxSqlSize(druidStatMaxKeySize);
									DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
									DruidDsConfHandle.this.druidDataSource = druidDataSource;
									tempDataSource.close();
									logger.warn("[DRUID CONFIG CHANGE STATUS] Always ReCreate DataSource !");
								} catch (Exception e) {
									logger.error(
											"[DRUID Create GlobaConf Error]  Always ReCreate DataSource Error !",
											e);
								}
							}else{
								logger.warn("[DRUID Create GlobaConf Error]  global config is same!nothing will be done! the global config is:"
										+ globaConfStr);
							}
						}
					}
					//�������ݿ�״̬������
					processDbStatusListener(DruidDsConfHandle.this.runTimeConf.getDbStautsEnum(),
							newConf.getDbStautsEnum());
					//�����µ����ø�������ʱ������
					DruidDsConfHandle.this.runTimeConf = newConf;
					clearDataSourceWrapper();
				} finally {
					lock.unlock();
				}
			}

			private boolean isGlobalChangeNeedReCreate(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				boolean needReCreate = false;
				if (!StringUtil.equals(runConf.getIp(), newConf.getIp())) {
					needReCreate = true;
					return needReCreate;
				}
				if (!StringUtil.equals(runConf.getPort(), newConf.getPort())) {
					needReCreate = true;
					return needReCreate;
				}
				if (!StringUtil.equals(runConf.getDbName(), newConf.getDbName())) {
					needReCreate = true;
					return needReCreate;
				}
				if (runConf.getDbTypeEnum() != newConf.getDbTypeEnum()) {
					needReCreate = true;
					return needReCreate;
				}
				
				return needReCreate;
			}
		});
	}

	/**
	 * Ӧ�����ü�������Ӧ�����÷����仯ʱ�����ַ��� �仯�����ã�������������flush����reCreate
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerAppDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerAppDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[DRUID AppConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String appConfStr = data;
					DruidDsConfDO tmpConf = DruidConfParser
							.parserTAtomDsConfDO(null, appConfStr);
					DruidDsConfDO newConf = DruidDsConfHandle.this.runTimeConf
							.clone();
					// ��Щ�������ò��ܱ�������Կ�¡�ϵ����ã�Ȼ���µ�set��ȥ
					newConf.setUserName(tmpConf.getUserName());
					newConf.setMinPoolSize(tmpConf.getMinPoolSize());
					newConf.setMaxPoolSize(tmpConf.getMaxPoolSize());
					newConf.setInitPoolSize(tmpConf.getInitPoolSize());
					newConf.setIdleTimeout(tmpConf.getIdleTimeout());
					newConf.setBlockingTimeout(tmpConf.getBlockingTimeout());
					newConf.setPreparedStatementCacheSize(tmpConf
							.getPreparedStatementCacheSize());
					newConf.setConnectionProperties(tmpConf
							.getConnectionProperties());
					newConf.setOracleConType(tmpConf.getOracleConType());
					// ����3�������ʵ��
					newConf.setWriteRestrictTimes(tmpConf
							.getWriteRestrictTimes());
					newConf.setReadRestrictTimes(tmpConf.getReadRestrictTimes());
					newConf.setThreadCountRestrict(tmpConf
							.getThreadCountRestrict());
					newConf.setTimeSliceInMillis(tmpConf.getTimeSliceInMillis());
					newConf.setDriverClass(tmpConf.getDriverClass());
					// ��������������
					overConfByLocal(DruidDsConfHandle.this.localConf, newConf);
					
					boolean isNeedReCreate = isAppChangeNeedReCreate(
							DruidDsConfHandle.this.runTimeConf, newConf);
					if (isNeedReCreate) {
						// ת��tAtomDsConfDO
						DruidDataSource druidDataSource;
						try {
							druidDataSource = convertTAtomDsConf2DruidConf(DruidDsConfHandle.this.dbKey,
									newConf, DruidConstants.getDbNameStr(
											DruidDsConfHandle.this.unitName,
											DruidDsConfHandle.this.appName,
											DruidDsConfHandle.this.dbKey));
						} catch (Exception e1) {
							logger.error("[DRUID GlobaConfError] convertTAtomDsConf2DruidConf Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ���ת�������Ƿ���ȷ
						if (!checkLocalTxDataSourceDO(druidDataSource)) {
							logger.error("[DRUID GlobaConfError] dataSource Prams Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						
						try {
							//�����������ǰ�棬�����´����Ϳ����޷��Ƚϳ���ͬ��������������
							DruidDsConfHandle.this.runTimeConf = newConf;
							DruidDsConfHandle.this.druidDataSource.close();
							logger.warn("[DRUID destroy OldDataSource] dataId : "
									+ dataId);
							druidDataSource.init();
//							druidDataSource.getDataSourceStat().setMaxSqlSize(DruidDsConfHandle.druidStatMaxKeySize);
							logger.warn("[DRUID create newDataSource] dataId : "
									+ dataId);
							DruidDsConfHandle.this.druidDataSource = druidDataSource;
							clearDataSourceWrapper();
						} catch (Exception e) {
							logger.error(
									"[DRUID Create GlobaConf Error]  Always ReCreate DataSource Error ! dataId: "
											+ dataId, e);
						}
					} else {
						boolean isNeedFlush = isAppChangeNeedFlush(
								DruidDsConfHandle.this.runTimeConf, newConf);
						
						if (isNeedFlush) {
							try {
								DruidDsConfHandle.this.runTimeConf = newConf;
								Properties prop=new Properties();
								prop.putAll(newConf.getConnectionProperties());
								DruidDsConfHandle.this.druidDataSource.setConnectProperties(prop);
								DruidDsConfHandle.this.druidDataSource.setMinIdle(newConf.getMinPoolSize());
								DruidDsConfHandle.this.druidDataSource.setMaxActive(newConf.getMaxPoolSize());
								if(newConf.getPreparedStatementCacheSize() > 0 && AtomDbTypeEnum.MYSQL != newConf.getDbTypeEnum()){
									DruidDsConfHandle.this.druidDataSource.setPoolPreparedStatements(true);
									DruidDsConfHandle.this.druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(newConf.getPreparedStatementCacheSize());
								}
								if (newConf.getIdleTimeout() > 0) {
									DruidDsConfHandle.this.druidDataSource.setTimeBetweenEvictionRunsMillis(newConf.getIdleTimeout()*60*1000);
									DruidDsConfHandle.this.druidDataSource.setMinEvictableIdleTimeMillis(newConf.getIdleTimeout()*60*1000);
								}
								if (newConf.getBlockingTimeout() > 0) {
									DruidDsConfHandle.this.druidDataSource.setMaxWait(newConf.getBlockingTimeout());
								}
								logger.info("[TDDL DRUID] flush ds success,dataId : "
										+ dataId);
								clearDataSourceWrapper();
							} catch (Exception e) {
								logger.error(
										"[TDDL DRUID] flush DataSource Error ! dataId:"
												+ dataId + ",data:"
												+ appConfStr, e);
							}
						} else {
							DruidDsConfHandle.this.runTimeConf = newConf;
							clearDataSourceWrapper();
						}
					}
				} finally {
					lock.unlock();
				}
			}

			private boolean isAppChangeNeedReCreate(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				if (!newConf.getDriverClass().equals(runConf.getDriverClass())){
					return true;
				}
				
				if (AtomDbTypeEnum.ORACLE == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					//oracle�����Ӳ����仯�ᵼ��
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
			
				if(!StringUtil.equals(runConf.getUserName(),newConf.getUserName())){
					return true;
				}
				
				if(runConf.getOracleConType()!=newConf.getOracleConType()){
					return true;
				}
				
				if(runConf.getDbTypeEnum()!=newConf.getDbTypeEnum()){
					return true;
				}
			    
				return false;
			}

			private boolean isAppChangeNeedFlush(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				if (AtomDbTypeEnum.MYSQL == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
				
				if (!StringUtil.equals(runConf.getPasswd(), newConf.getPasswd())) {
					return true;
				}
				
				if (runConf.getMinPoolSize() != newConf.getMinPoolSize()) {
					return true;
				}

				if (runConf.getMaxPoolSize() != newConf.getMaxPoolSize()) {
					return true;
				}

				if (runConf.getIdleTimeout() != newConf.getIdleTimeout()) {
					return true;
				}
				
				if (runConf.getBlockingTimeout()!= newConf.getBlockingTimeout()) {
					return true;
				}
				
				if (runConf.getPreparedStatementCacheSize()!= newConf.getPreparedStatementCacheSize()) {
					return true;
				}
				
				return false;
			}
		});
	}
	
	/**
	 * druid���⿪�أ�����������ת����Ŀǰ��B2B����վר��
	 * @param connectionProperties
	 * @param druidDataSource
	 * @throws SQLException
	 */
	protected static void fillDruidFilters(Map<String, String> connectionProperties, DruidDataSource druidDataSource) throws SQLException{
		if(connectionProperties.containsKey("clientEncoding") 
				|| connectionProperties.containsKey("serverEncoding")){
			druidDataSource.setFilters(TDDL_DRUID_ENCODING_FILTER);
		}
	}
	
	private static final String TDDL_DRUID_ENCODING_FILTER="encoding";
//	private static final String DEFAULT_TDDL_DRUID_FILTERS="mergeStat";
	
	/**
	 * ��TAtomDsConfDOת����LocalTxDataSourceDO
	 * 
	 * @param tAtomDsConfDO
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected static DruidDataSource convertTAtomDsConf2DruidConf(String dbKey, DruidDsConfDO tAtomDsConfDO, String dbName) throws Exception{
		DruidDataSource localDruidDataSource = new DruidDataSource();
		//һ��������druid�����Ҫ����������
		localDruidDataSource.setName(dbKey);
		localDruidDataSource.setTestOnBorrow(false);
		localDruidDataSource.setTestWhileIdle(true);
		
//		localDruidDataSource.setFilters(DEFAULT_TDDL_DRUID_FILTERS);
		localDruidDataSource.setUsername(tAtomDsConfDO.getUserName());
		localDruidDataSource.setPassword(tAtomDsConfDO.getPasswd());
		localDruidDataSource.setDriverClassName(tAtomDsConfDO.getDriverClass());
		localDruidDataSource.setExceptionSorterClassName(tAtomDsConfDO.getSorterClass());
		//�������ݿ���������conURL��setConnectionProperties
		if (AtomDbTypeEnum.ORACLE == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = DruidConURLTools.getOracleConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getOracleConType());
			localDruidDataSource.setUrl(conUlr);
			//�����oracleû������ConnectionProperties����Ը�Ĭ�ϵ�
			Properties connectionProperties = new Properties();
			if (!tAtomDsConfDO.getConnectionProperties().isEmpty()) {
				connectionProperties.putAll(tAtomDsConfDO.getConnectionProperties());
				fillDruidFilters(tAtomDsConfDO.getConnectionProperties(), localDruidDataSource);
			} else {
				connectionProperties.putAll(DruidConstants.DEFAULT_ORACLE_CONNECTION_PROPERTIES);
			}
			localDruidDataSource.setConnectProperties(connectionProperties);
			localDruidDataSource.setValidationQuery(DruidConstants.DEFAULT_DRUID_ORACLE_VALIDATION_QUERY);
		} else if (AtomDbTypeEnum.MYSQL == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = DruidConURLTools.getMySqlConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getConnectionProperties());
			localDruidDataSource.setUrl(conUlr);
			//��������ҵ�mysqlDriver�е�Valid��ʹ�ã���������valid
			try {
				Class validClass = Class.forName(DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				if (null != validClass) {
					localDruidDataSource
							.setValidConnectionCheckerClassName(DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				} else {
					logger.warn("MYSQL Driver is Not Suport "
							+ DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			} catch (NoClassDefFoundError e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			}
			
			//��������ҵ�mysqlDriver�е�integrationSorter��ʹ�÷���ʹ��Ĭ�ϵ�
			try {
				Class integrationSorterCalss = Class.forName(DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS);
				if (null != integrationSorterCalss) {
					localDruidDataSource.setExceptionSorterClassName(DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS);
				} else {
					localDruidDataSource.setExceptionSorterClassName(DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
					logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
							+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
			} catch (NoClassDefFoundError e){
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
			}
			localDruidDataSource.setValidationQuery(DruidConstants.DEFAULT_DRUID_MYSQL_VALIDATION_QUERY);
		}
		// lazy init ������Ϊ0 ��������ִ��ʱ�Ŵ�������
		localDruidDataSource.setInitialSize(tAtomDsConfDO.getInitPoolSize());
		localDruidDataSource.setMinIdle(tAtomDsConfDO.getMinPoolSize());
		localDruidDataSource.setMaxActive(tAtomDsConfDO.getMaxPoolSize());
		if(tAtomDsConfDO.getPreparedStatementCacheSize() > 0 && AtomDbTypeEnum.MYSQL != tAtomDsConfDO.getDbTypeEnum()){
			localDruidDataSource.setPoolPreparedStatements(true);
			localDruidDataSource.setMaxPoolPreparedStatementPerConnectionSize(tAtomDsConfDO.getPreparedStatementCacheSize());
		}
		if (tAtomDsConfDO.getIdleTimeout() > 0) {
			localDruidDataSource.setTimeBetweenEvictionRunsMillis(tAtomDsConfDO.getIdleTimeout()*60*1000);
			localDruidDataSource.setMinEvictableIdleTimeMillis(tAtomDsConfDO.getIdleTimeout()*60*1000);
		}
		if (tAtomDsConfDO.getBlockingTimeout() > 0) {
			localDruidDataSource.setMaxWait(tAtomDsConfDO.getBlockingTimeout());
		}
		
		if(tAtomDsConfDO.getConnectionInitSql() != null) {
		    localDruidDataSource.setConnectionInitSqls(Arrays.asList(tAtomDsConfDO.getConnectionInitSql()));
		}
		//���druid��־���
//		DruidDataSourceStatLogger logger=localDruidDataSource.getStatLogger();
//		logger.setLogger(new Log4jImpl(LoggerInit.TDDL_Atom_Statistic_LOG));
//		localDruidDataSource.setTimeBetweenLogStatsMillis(DruidDsConfHandle.druidFlushIntervalMill);
		
		return localDruidDataSource;
	}

	protected static boolean checkLocalTxDataSourceDO(
			DruidDataSource druidDataSource) {
		if (null == druidDataSource) {
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getUrl())) {
			logger.error("[DsConfig Check] URL is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getUsername())) {
			logger.error("[DsConfig Check] Username is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getPassword())) {
			logger.error("[DsConfig Check] Password is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getDriverClassName())) {
			logger.error("[DsConfig Check] DriverClassName is Empty !");
			return false;
		}

		if (druidDataSource.getMinIdle() < 1) {
			logger.error("[DsConfig Check] MinIdle Error size is:"
					+ druidDataSource.getMinIdle());
			return false;
		}
		
		if (druidDataSource.getMaxActive() < 1) {
			logger.error("[DsConfig Check] MaxActive Error size is:"
					+ druidDataSource.getMaxActive());
			return false;
		}

		if (druidDataSource.getMinIdle() > druidDataSource.getMaxActive()) {
			logger.error("[DsConfig Check] MinPoolSize Over MaxPoolSize Minsize is:"
					+ druidDataSource.getMinIdle()
					+ "MaxSize is :"
					+ druidDataSource.getMaxActive());
			return false;
		}
		return true;
	}

	/**
	 * ���ñ������ø��Ǵ����TAtomDsConfDO������
	 * 
	 * @param tAtomDsConfDO
	 */
	private void overConfByLocal(DruidDsConfDO localDsConfDO,
			DruidDsConfDO newDsConfDO) {
		if (null == newDsConfDO || null == localDsConfDO) {
			return;
		}
		//��������driverClass
//		if (StringUtil.isNotBlank(localDsConfDO.getDriverClass())) {
//			newDsConfDO.setDriverClass(localDsConfDO.getDriverClass());
//		}
		if (StringUtil.isNotBlank(localDsConfDO.getSorterClass())) {
			newDsConfDO.setSorterClass(localDsConfDO.getSorterClass());
		}
		if (StringUtil.isNotBlank(localDsConfDO.getPasswd())) {
			newDsConfDO.setPasswd(localDsConfDO.getPasswd());
		}
		if (null != localDsConfDO.getConnectionProperties()
				&& !localDsConfDO.getConnectionProperties().isEmpty()) {
			newDsConfDO.setConnectionProperties(localDsConfDO
					.getConnectionProperties());
		}
	}

	/**
	 * Datasource �İ�װ��
	 */
	private volatile TDataSourceWrapper wrapDataSource = null;

	public DataSource getDataSource() throws SQLException {
		if (wrapDataSource == null) {
			lock.lock();
			try {
				if (wrapDataSource != null) {
					// ˫�����
					return wrapDataSource;
				}
				String errorMsg = "";
				if (null == druidDataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe forget init !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				DataSource dataSource = druidDataSource;
				if (null == dataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe init fail !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				// ������ݿ�״̬������ֱ���׳��쳣
				if (null == this.getStatus()) {
					errorMsg = "[DB Stats Error] DbStatus is Null: "
							+ this.getDbKey();
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				TDataSourceWrapper tDataSourceWrapper = new TDataSourceWrapper(
						dataSource, runTimeConf);
				tDataSourceWrapper.setDatasourceName(dbKey);
				tDataSourceWrapper.setDatasourceIp(runTimeConf.getIp());
				tDataSourceWrapper.setDatasourcePort(runTimeConf.getPort());
				tDataSourceWrapper.setDatasourceRealDbName(runTimeConf.getDbName());
				tDataSourceWrapper.setDbStatus(getStatus());
				logger.warn("set datasource key: " + dbKey);
				wrapDataSource = tDataSourceWrapper;

				return wrapDataSource;

			} finally {
				lock.unlock();
			}
		} else {
			return wrapDataSource;
		}
	}

	public void flushDataSource() {
		//��ʱ��֧��flush �״�
		logger.error("DRUID DATASOURCE DO NOT SUPPORT FLUSH.");
		throw new RuntimeException("DRUID DATASOURCE DO NOT SUPPORT FLUSH.");
	}

	protected void destroyDataSource() throws Exception {
		if (null != this.druidDataSource) {
			logger.warn("[DataSource Stop] Start!");
			this.druidDataSource.close();
			if (null != this.dbConfManager) {
				this.dbConfManager.stopDbConfManager();
			}
			if (null != this.dbPasswdManager) {
				this.dbPasswdManager.stopDbPasswdManager();
			}
			logger.warn("[DataSource Stop] End!");
		}

	}

	void setSingleInGroup(boolean isSingleInGroup) {
		this.runTimeConf.setSingleInGroup(isSingleInGroup);
	}

	public void setAppName(String appName) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset appName !");
		}
		this.appName = appName;
	}

	public void setDbKey(String dbKey) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset dbKey !");
		}
		this.dbKey = dbKey;
	}

	public void setLocalPasswd(String passwd) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset passwd !");
		}
		this.localConf.setPasswd(passwd);
	}

	public void setLocalConnectionProperties(Map<String, String> map)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset connectionProperties !");
		}
		this.localConf.setConnectionProperties(map);
		String driverClass = map.get(DruidConfParser.APP_DRIVER_CLASS_KEY);
		if (!StringUtil.isBlank(driverClass)) {
			this.localConf.setDriverClass(driverClass);
		}
	}

	public void setLocalDriverClass(String driverClass)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset driverClass !");
		}
		this.localConf.setDriverClass(driverClass);
	}

	public void setLocalSorterClass(String sorterClass)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset sorterClass !");
		}
		this.localConf.setSorterClass(sorterClass);
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public String getAppName() {
		return appName;
	}

	public String getDbKey() {
		return dbKey;
	}

	public AtomDbStatusEnum getStatus() {
		return this.runTimeConf.getDbStautsEnum();
	}

	public AtomDbTypeEnum getDbType() {
		return this.runTimeConf.getDbTypeEnum();
	}

	public void setDbStatusListeners(
			List<DruidDbStatusListener> dbStatusListeners) {
		this.dbStatusListeners = dbStatusListeners;
	}

	private void processDbStatusListener(AtomDbStatusEnum oldStatus,
			AtomDbStatusEnum newStatus) {
		if (null != oldStatus && oldStatus != newStatus) {
			if (null != dbStatusListeners) {
				for (DruidDbStatusListener statusListener : dbStatusListeners) {
					try {
						statusListener.handleData(oldStatus, newStatus);
					} catch (Exception e) {
						logger.error("[call StatusListenner Error] !", e);
						continue;
					}
				}
			}
		}
	}
}
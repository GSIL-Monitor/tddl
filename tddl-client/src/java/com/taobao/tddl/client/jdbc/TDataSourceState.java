package com.taobao.tddl.client.jdbc;

import java.util.concurrent.ThreadPoolExecutor;

import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.monitor.SnapshotValuesOutputCallBack;
import com.taobao.tddl.common.monitor.stat.StatLogWriter;
import com.taobao.tddl.parser.ParserCache;

/**
 * TDDL �ڲ�����״̬���ʽӿ�
 * 
 * @author linxuan
 *
 */
public class TDataSourceState implements SnapshotValuesOutputCallBack {
	private ThreadPoolExecutor replicationExecutor;
	private Integer replicationQueueSize;
	private final String dataSourceName;

	public TDataSourceState(String dataSourceName) {
		this.dataSourceName = dataSourceName;
		Monitor.addSnapshotValuesCallbask(this);
	}

	public int getReplicationMinPoolSize() {
		return replicationExecutor == null ? -1 : replicationExecutor.getCorePoolSize();
	}

	public int getReplicationMaxPoolSize() {
		return replicationExecutor == null ? -1 : replicationExecutor.getMaximumPoolSize();
	}

	public int getReplicationCurrentPoolSize() {
		return replicationExecutor == null ? -1 : replicationExecutor.getPoolSize();
	}

	public int getReplicationCurrentQueueSize() {
		return replicationExecutor == null ? -1 : replicationExecutor.getQueue().size();
	}

	public int getReplicationMaxQueueSize() {
		return replicationQueueSize == null ? -1 : replicationQueueSize;
	}

	public int getParserCacheSize() {
		return ParserCache.instance().size();
	}

	@Override
	public void snapshotValues(StatLogWriter statLog) {
		// ���ƶ��г���: ��ǰ����/��󳤶�
		statLog.log(dataSourceName + Key.replicationQueueSize, 
		        getReplicationCurrentQueueSize(), getReplicationMaxQueueSize());

		// �����̳߳ش�С�� ��ǰ�߳���/����߳���
		statLog.log(dataSourceName + Key.replicationPoolSize, 
		        getReplicationCurrentPoolSize(), getReplicationMaxPoolSize());

		// ���������С����ǰ��С/�������
		statLog.log(dataSourceName + Key.parserCacheSize, 
		        getParserCacheSize(), ParserCache.instance().capacity);
	}

	/**
	 * Setter
	 */
	public void setReplicationExecutor(ThreadPoolExecutor replicationExecutor) {
		this.replicationExecutor = replicationExecutor;
	}

	public void setReplicationQueueSize(Integer replicationQueueSize) {
		this.replicationQueueSize = replicationQueueSize;
	}
}

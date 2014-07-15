/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.taobao.tddl.common.monitor;

import com.taobao.tddl.common.monitor.stat.StatLogWriter;

/**
 * 
 * һЩ��ֵ̬�Ĵ�����ֵ̬�������ۼ�ģ�ͣ�����������ʱ��ص�����ӿڼ��뵽old�������
 * 
 * @author changyuan.lh
 * @author shenxun
 * @author junyu
 */
public interface SnapshotValuesOutputCallBack {
	public static class Key {
		public static final String replicationQueueSize = "_replicationQueueSize";
		public static final String replicationPoolSize = "_replicationPoolSize";
		public static final String parserCacheSize = "_parserCacheSize";

		public static final String THREAD_COUNT = "THREAD_COUNT";
		public static final String THREAD_COUNT_REJECT_COUNT = "THREAD_COUNT_REJECT_COUNT";
		public static final String READ_WRITE_TIMES = "READ_WRITE_TIMES";
		public static final String READ_WRITE_TIMES_REJECT_COUNT = "READ_WRITE_TIMES_REJECT_COUNT";
		public static final String READ_WRITE_CONCURRENT = "READ_WRITE_CONCURRENT";
	}

	/**
	 * ��ǰ��ͳ�����ݻ��ܣ�
	 * 
	 * @see TDataSourceState
	 * @see TDataSourceWrapper
	 */
	void snapshotValues(StatLogWriter statLog);
}

package com.taobao.tddl.common.standard.jdbc;

import com.taobao.tddl.common.channel.SqlMetaData;

/**
 * @author JIECHEN
 */
public interface DataChannel{
	
	/**
	 * ���ݸ�sql��Ԫ��Ϣ���ײ�
	 * @param sqlMetaData
	 */
	public void fillMetaData(SqlMetaData sqlMetaData);
	
	public SqlMetaData getSqlMetaData();
	
}

package com.taobao.tddl.common.channel;

import java.util.List;

public interface SqlMetaData {
	
	/**
	 * ԭʼ��sql,��Ӧ��ֱ�Ӹ����ײ��sql
	 * @return
	 */
	public String getOriSql();
	
	/**
	 * ��Ը�ʽ���߲���������ͬ�����������һ�µ�sql��ͳһ��ʽ��
	 * ���� id in(?,?...) ͳһΪ id in (?)
	 * @return
	 */
	public String getLogicSql();
	
	/**
	 * @return
	 */
	public List<String> getLogicTables();
	
	/**
	 * sql�Ƿ񱻽�����
	 * @return
	 */
	public boolean isParsed();

}

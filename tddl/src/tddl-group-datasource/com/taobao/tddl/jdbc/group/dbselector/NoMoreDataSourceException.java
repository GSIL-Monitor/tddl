package com.taobao.tddl.jdbc.group.dbselector;

import java.sql.SQLException;

/**
 * ��һ������ݿⶼ�Թ������������ˣ�����û�и��������Դ�ˣ��׳��ô���
 * 
 * @author linxuan
 * 
 */
public class NoMoreDataSourceException extends SQLException {

	private static final long serialVersionUID = 1L;

	public NoMoreDataSourceException(String reason) {
		super(reason + 
		    "\nPlease grep 'createSQLException' or 'createCommunicationsException' or 'time out' to find real causes." + 
		        " And you can reference " +
		        "'http://baike.corp.taobao.com/index.php/TDDL_FAQ'");
	}
	
}

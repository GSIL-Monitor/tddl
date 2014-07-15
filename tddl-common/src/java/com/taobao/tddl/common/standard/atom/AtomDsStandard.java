package com.taobao.tddl.common.standard.atom;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.sql.DataSource;

public interface AtomDsStandard extends DataSource{

	/**
	 * @param appName
	 * @param dsKey
	 */
	public void init(String appName, String dsKey, String unitName) throws Exception;

	public void setLogWriter(PrintWriter out) throws SQLException;
	
	public void setLoginTimeout(int seconds) throws SQLException;
	
	public void setShutDownMBean(boolean shutDownMBean);
	
	public AtomDbTypeEnum getDbType();
	
	public AtomDbStatusEnum getDbStatus();
	
	public void destroyDataSource() throws Exception;

}

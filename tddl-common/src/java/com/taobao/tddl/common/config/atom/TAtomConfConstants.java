package com.taobao.tddl.common.config.atom;

import java.text.MessageFormat;

import com.taobao.tddl.common.util.StringUtils;

/**
 * atom ds�������ȡ������ص�key
 * @author JIECHEN
 *
 */
public class TAtomConfConstants {

	/**
	 * ȫ������dataIdģ��
	 */
	private static final MessageFormat GLOBAL_FORMAT = new MessageFormat(
			"com.taobao.tddl.atom.global.{0}");

	/**
	 * Ӧ������dataIdģ��
	 */
	private static final MessageFormat APP_FORMAT = new MessageFormat(
			"com.taobao.tddl.atom.app.{0}.{1}");

	private static final MessageFormat PASSWD_FORMAT = new MessageFormat(
			"com.taobao.tddl.atom.passwd.{0}.{1}.{2}");

	/**
	 * dbNameģ��
	 */
	private static final MessageFormat DB_NAME_FORMAT = new MessageFormat(
			"atom.dbkey.{0}^{1}^{2}");
	
	private static final String NULL_UNIT_NAME = "DEFAULT_UNIT";
	

	/**
	 * ����dbKey��ȡȫ������dataId
	 *
	 * @param dbKey
	 *            ���ݿ���KEY
	 * @return
	 */
	public static String getGlobalDataId(String dbKey) {
		return GLOBAL_FORMAT.format(new Object[] { dbKey });
	}

	/**
	 * ����Ӧ������dbKey��ȡָ����Ӧ������dataId
	 *
	 * @param appName
	 * @param dbKey
	 * @return
	 */
	public static String getAppDataId(String appName, String dbKey) {
		return APP_FORMAT.format(new Object[] { appName, dbKey });
	}

	/**
	 * ����dbKey��userName��ö�Ӧ��passwd��dataId
	 *
	 * @param dbKey
	 * @param userName
	 * @return
	 */
	public static String getPasswdDataId(String dbName, String dbType,
			String userName) {
		return PASSWD_FORMAT.format(new Object[] { dbName, dbType, userName });
	}
	
	/**
	 * @param appName
	 * @param unitName
	 * @param dbkey
	 * @return
	 */
	public static String getDbNameStr(String unitName, String appName, String dbkey) {
		if(StringUtils.nullOrEmpty(unitName))
			return DB_NAME_FORMAT.format(new Object[] { NULL_UNIT_NAME, appName, dbkey });
		
		return DB_NAME_FORMAT.format(new Object[] { unitName , appName, dbkey });
	}

}

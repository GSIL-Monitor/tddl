package com.taobao.tddl.jdbc.atom;

import com.taobao.tddl.common.client.util.ThreadLocalMap;

/**
 * �ṩ������ʹ��  TAtomDataSource ���û�ָ��Ӧ���������Ƶ�ҵ���  (Key) �Լ�����ִ����Ϣ��
 * 
 * @author changyuan.lh
 */
public class AtomDataSourceHelper {

	/**
	 * ָ��Ӧ���������Ƶ�ҵ��� (Key)
	 */
	public static final String CONN_RESTRICT_KEY = "CONN_RESTRICT_KEY";

	public static void setConnRestrictKey(Object key) {
		ThreadLocalMap.put(AtomDataSourceHelper.CONN_RESTRICT_KEY, key);
	}

	public static Object getConnRestrictKey() {
		return ThreadLocalMap.get(AtomDataSourceHelper.CONN_RESTRICT_KEY);
	}

	public static void removeConnRestrictKey() {
		ThreadLocalMap.remove(AtomDataSourceHelper.CONN_RESTRICT_KEY);
	}
}

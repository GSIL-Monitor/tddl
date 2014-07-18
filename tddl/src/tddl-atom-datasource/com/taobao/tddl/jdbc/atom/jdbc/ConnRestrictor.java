package com.taobao.tddl.jdbc.atom.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.jboss.util.NestedSQLException;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.jdbc.atom.AtomDataSourceHelper;

/**
 * Ӧ���������Ƶ���Ҫ�߼�ʵ�֡�
 */
public class ConnRestrictor {

	/**
	 * MAP ���Ե�Ӧ����������, ��ȷ��ƥ�� Key �����Ӳۡ�
	 */
	private HashMap<String, ConnRestrictSlot> mapConnRestrict;

	/**
	 * HASH ���Ե�Ӧ����������, �� Hash + ȡģ�ķ�ʽƥ�� Key �����Ӳۡ�
	 */
	private ConnRestrictSlot[] hashConnRestrict;

	/**
	 * û�ж���ҵ��� (null Key) ���������Ʋۡ�
	 */
	private ConnRestrictSlot nullKeyRestrictSlot;

	/**
	 * ��ʼ��Ӧ���������Ƶ����ݽṹ, ��Щ���ݽṹֻ�ᱻ��ʼ��һ�Ρ�
	 */
	public ConnRestrictor(String datasourceKey, List<ConnRestrictEntry> connRestrictEntries) {
		for (ConnRestrictEntry connRestrictEntry : connRestrictEntries) {
			String[] slotKeys = connRestrictEntry.getKeys();
			if (slotKeys.length == 1
					&& ConnRestrictEntry.isWildcard(slotKeys[0])) {
				int maxHashSize = connRestrictEntry.getHashSize();
				if (maxHashSize < 1) {
					maxHashSize = 1;
				}
				if (maxHashSize > ConnRestrictEntry.MAX_HASH_RESTRICT_SLOT) {
					maxHashSize = ConnRestrictEntry.MAX_HASH_RESTRICT_SLOT;
				}
				if (hashConnRestrict == null) {
					// ÿ�� HASH ��Ƭ���ö����Ĳ�
					hashConnRestrict = new ConnRestrictSlot[maxHashSize];
					for (int i = 0; i < maxHashSize; i++) {
						hashConnRestrict[i] = new ConnRestrictSlot(datasourceKey, 
								"*:" + i, connRestrictEntry);
					}
				}
			} else if (slotKeys.length == 1
					&& ConnRestrictEntry.isNullKey(slotKeys[0])) {
				if (nullKeyRestrictSlot == null) {
					nullKeyRestrictSlot = new ConnRestrictSlot(datasourceKey, 
							slotKeys[0], connRestrictEntry);
				}
			} else {
				// ע��, ������ҵ���ͬʱ������һ����
				ConnRestrictSlot connRestrictSlot = new ConnRestrictSlot(datasourceKey, 
						StringUtil.join(slotKeys, '|'), connRestrictEntry);
				if (mapConnRestrict == null) {
					mapConnRestrict = new HashMap<String, ConnRestrictSlot>();
				}
				for (String slotKey : slotKeys) {
					if (ConnRestrictEntry.isNullKey(slotKey)) {
						if (nullKeyRestrictSlot == null) {
							nullKeyRestrictSlot = connRestrictSlot;
						}
					} else if (!ConnRestrictEntry.isWildcard(slotKey)) {
						if (!mapConnRestrict.containsKey(slotKey)) {
							mapConnRestrict.put(slotKey, connRestrictSlot);
						}
					}
				}
			}
		}
	}

	/**
	 * �����ݽṹ�в���Ӧ���������ƵĲۡ�
	 */
	public ConnRestrictSlot findSlot(Object connKey) {
		if (connKey != null) {
			ConnRestrictSlot connRestrictSlot = null;
			if (mapConnRestrict != null) {
				// ���Ⱦ�ȷƥ��
				connRestrictSlot = mapConnRestrict.get(String.valueOf(connKey));
			}
			if (connRestrictSlot == null) {
				if (hashConnRestrict != null) {
					// ���û�о�ȷָ��, ���� HASH ��ʽ
					final int hash = Math.abs(connKey.hashCode()
							% hashConnRestrict.length);
					connRestrictSlot = hashConnRestrict[hash];
				}
			}
			return connRestrictSlot;
		}
		// û�ж���ҵ���, �� null Key ��
		return nullKeyRestrictSlot;
	}

	/**
	 * Ӧ���������Ƶ���ں�����
	 */
	public ConnRestrictSlot doRestrict(final int timeoutInMillis)
			throws SQLException {
		final Object connKey = AtomDataSourceHelper.getConnRestrictKey();
		ConnRestrictSlot connRestrictSlot = findSlot(connKey);
		try {
			// ���û��ƥ��Ĳ�, ��������
			if (connRestrictSlot != null) {
				if (!connRestrictSlot.allocateConnection(timeoutInMillis)) {
					// ������ʱ
					throw new NestedSQLException(
							"No connection available for '" + connKey
									+ "' within configured blocking timeout ("
									+ timeoutInMillis + "[ms])");
				}
			}
		} catch (InterruptedException e) {
			throw new NestedSQLException("Allocate connection for '" + connKey
					+ "' interrupted within configured blocking timeout ("
					+ timeoutInMillis + "[ms])", e);
		} catch (RuntimeException e) {
			throw new NestedSQLException("Allocate connection for '" + connKey
					+ "' failed: unexpected exception", e);
		}
		return connRestrictSlot;
	}
}

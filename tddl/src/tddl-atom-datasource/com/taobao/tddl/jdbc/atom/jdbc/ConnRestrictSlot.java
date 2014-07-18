package com.taobao.tddl.jdbc.atom.jdbc;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.monitor.stat.AbstractStatLogWriter.LogCounter;

/**
 * ʵ��Ӧ�����������ƹ�����, ����ĳһ���� (Slot) �����������ơ�
 * 
 * @author changyuan.lh
 */
public final class ConnRestrictSlot {

	private final ConnRestrictEntry entry;

	/**
	 * ֱ���� �ź���, ����һ�����ǻ��� AbstractQueuedSynchronizer, ����Ӧ��
	 * û������, ���ǲ��ܶ�̬�� permits���������ڵ����ͻ�����ֱ�Ӷ����ɵ� 
	 * TDataSourceWrapper �����µ�: �ɵ����ӻ����ɵ� Slot, �µ���������
	 * ���� Slot, ���Կ���û�ж�̬�ı�Ҫ��
	 */
	private final Semaphore semaphore;

	// changyuan.lh: �����������������ȴ���ͳ�ƶ���
	private final LogCounter statConnNumber;
	private final LogCounter statConnBlocking;

	public ConnRestrictSlot(String datasourceKey, String slotKey, ConnRestrictEntry entry) {
		this.statConnNumber = Monitor.connStat(datasourceKey, slotKey, Monitor.KEY3_CONN_NUMBER);
		this.statConnBlocking = Monitor.connStat(datasourceKey, slotKey, Monitor.KEY3_CONN_BLOCKING);
		this.semaphore = new Semaphore(entry.limits); // Nofair, �� Spin ���ܺ�һЩ
		this.entry = entry;
	}

	/**
	 * changyuan.lh: ��¼ͳ����Ϣ
	 */
	public void statConnection(final long connMillis) {
		statConnNumber.stat(1, semaphore.availablePermits());
		statConnBlocking.stat(1, connMillis);
	}

	public boolean allocateConnection(final int timeoutInMillis)
			throws InterruptedException {
		return semaphore.tryAcquire(timeoutInMillis, TimeUnit.MILLISECONDS);
	}

	public int getAvailableConnections() {
		return semaphore.availablePermits();
	}

	public int getConnections() {
		return entry.limits - semaphore.availablePermits();
	}

	public int getLimits() {
		return entry.limits;
	}

	public void freeConnection() {
		semaphore.release();
	}

	public String toString() {
		return "ConnRestrictSlot: @" + Integer.toHexString(hashCode()) + " "
				+ Arrays.toString(entry.keys) + " " + entry.limits;
	}
}

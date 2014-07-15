package com.taobao.tddl.common.monitor.stat;

/**
 * ͳ����Ϣ����־����ӿڡ� <br />
 * 
 * ���ߵĹ����Ƕ���ͬĿ���ͳ�����ݺϲ����, ����� min/max/avg, ��ʱˢ������־�� <br />
 * 
 * <pre>
 * StatLogWriter.write(
 *     new Object[] { key1, key2, key3, ... },  // ͳ�Ƶ�Ŀ��
 *     number1, number2, ...);                  // ͳ��ֵ
 * 
 * StatLogWriter.write(
 *     new Object[] { key1, key2, key3, ... },  // ͳ�Ƶ�Ŀ��
 *     new Object[] { obj1, obj2, obj3, ... },  // ʵ�ʵ������
 *     number1, number2, ...);                  // ͳ��ֵ
 * </pre>
 * 
 * @author changyuan.lh
 */
public abstract class StatLogWriter {

	public final void write(Object[] keys, long... values) {
		write(keys, keys, values);
	}

	/**
	 * ͳ����Ϣ�ֳ������֣�����Ŀ��/key, ͳ��Ŀ����Ϣ/fields, ����/values.
	 * 
	 * �����������ݻ���Ŀ��/key ���ڴ��л㼯��־�����Լ�����־��, Ȼ��ˢ������־�ļ���
	 * 
	 * @param keys
	 *            ���ܵ�Ŀ��, ͨ��������������־��ͳ��Ŀ����Ϣ/fields ��ͬ��
	 * 
	 * @param fields
	 *            ��־��¼�ֶ�, ��������������־�����ݺ�˳���������ٰ�������Ŀ��/key.
	 * 
	 * @param values
	 *            �����ͳ������, Լ����һ��ֵ������, �������ͳ��ֵ (RT, ������, etc).
	 */
	public abstract void write(Object[] keys, Object[] fields, // NL
			long... values);

	/* ���ͳ��ֵ�ļ�������� */
	public final void log(Object key, long... values) {
		Object[] keys = new Object[] { key };
		write(keys, keys, values);
	}

	public final void log2(Object key1, Object key2, long... values) {
		Object[] keys = new Object[] { key1, key2 };
		write(keys, keys, values);
	}

	public final void log3(Object key1, Object key2, Object key3, long... values) {
		Object[] keys = new Object[] { key1, key2, key3 };
		write(keys, keys, values);
	}

	/* ����/ͳ��ֵ�ļ�������� */
	public final void stat(Object key, long value) {
		Object[] keys = new Object[] { key };
		write(keys, keys, new long[] { 1L, value });
	}

	public final void stat(Object key, long count, long value) {
		Object[] keys = new Object[] { key };
		write(keys, keys, new long[] { count, value });
	}

	public final void stat(Object key1, Object key2, long value) {
		Object[] keys = new Object[] { key1, key2 };
		write(keys, keys, new long[] { 1L, value });
	}

	public final void stat(Object key1, Object key2, long count, long value) {
		Object[] keys = new Object[] { key1, key2 };
		write(keys, keys, new long[] { count, value });
	}

	public final void stat(Object key1, Object key2, Object key3, long value) {
		Object[] keys = new Object[] { key1, key2, key3 };
		write(keys, keys, new long[] { 1L, value });
	}

	public final void stat(Object key1, Object key2, Object key3, // NL
			long count, long value) {
		Object[] keys = new Object[] { key1, key2, key3 };
		write(keys, keys, new long[] { count, value });
	}
}

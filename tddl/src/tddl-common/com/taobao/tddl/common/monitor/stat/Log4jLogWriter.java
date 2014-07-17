package com.taobao.tddl.common.monitor.stat;

import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;

/**
 * ���� Log4j Logger ��ͳ����־����ࡣ
 * 
 * ��֤���¼���, ��־ˢ���ĽṹΪ��
 * 
 * <pre>
 * key1(sql)	key2(dbname)	key3(flag)	val1(count)	val2(sum)		val3(min)		val4(max)		time
 * sql		logicDbName	ִ�гɹ�		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		realDbName1	ִ�гɹ�		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		realDbName2	ִ�гɹ�		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		realDbName2	ִ��ʧ��		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		realDbName2	ִ�г�ʱ		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		null		�����ɹ�		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		null		����ʧ��		ִ�д���		��Ӧʱ��		��С��Ӧʱ��	�����Ӧʱ��	��־ʱ��
 * sql		null		��������		ִ�д���		���д���		NA		NA		��־ʱ��
 * </pre>
 * 
 * @author changyuan.lh
 */
public class Log4jLogWriter extends StatLogWriter {

	/** XXX: �ĳ� commons-lang �Դ��� {@link FastDateFormat}, ��������̰߳�ȫ�� */
	public static final FastDateFormat df = FastDateFormat
			.getInstance("yyy-MM-dd HH:mm:ss:SSS");

	protected String fieldSeperator = "#@#"; // SQL �г��ָ���С, ������ʽ����ͻ
	protected String lineSeperator = System.getProperty("line.separator");
	protected final Logger statLogger;

	public Log4jLogWriter(Logger statLogger) {
		this.statLogger = statLogger;
	}

	public Log4jLogWriter(String fieldSeperator, Logger statLogger) {
		this.fieldSeperator = fieldSeperator;
		this.statLogger = statLogger;
	}

	public Log4jLogWriter(String fieldSeperator, String lineSeperator,
			Logger statLogger) {
		this.fieldSeperator = fieldSeperator;
		this.lineSeperator = lineSeperator;
		this.statLogger = statLogger;
	}

	// XXX: ���������д��Ϣ, Ȼ��д����, ���дʱ��, ����������
	protected StringBuffer format(StringBuffer buf, Object[] fields, Date time,
			long... values) {
		for (Object field : fields) {
			buf.append(field).append(fieldSeperator);
		}
		for (long value : values) {
			buf.append(value).append(fieldSeperator);
		}
		return buf.append(df.format(time)).append(lineSeperator);
	}

	public void write(Object[] keys, Object[] fields, long... values) {
		StringBuffer buf = new StringBuffer();
		format(buf, (fields == null) ? keys : fields, new Date(), values);
		statLogger.warn(buf.toString());
	}
}

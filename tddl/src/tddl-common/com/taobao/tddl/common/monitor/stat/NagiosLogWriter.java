package com.taobao.tddl.common.monitor.stat;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.common.util.NagiosUtils;

/**
 * ���� Nagios ��ʽ��ͳ����־����ࡣ <br />
 * 
 * ���� StatMonitor �Ĵ��롣
 * 
 * @author changyuan.lh
 */
public class NagiosLogWriter extends StatLogWriter {

	public void write(Object[] keys, Object[] fields, long... values) {
		if (values.length < 2) {
			throw new IllegalArgumentException("At least given 2 values");
		}
		// XXX: ���� StatMonitor �����, ���� min/max ֻ���ƽ��ֵ
		long count = values[0];
		long value = values[1];
		String averageValueStr = "invalid";
		if (count != 0) {
			double averageValue = (double) value / count;
			averageValueStr = String.valueOf(averageValue);
		}
		if (fields == null) {
			fields = keys;
		}
		// NagiosUtils.addNagiosLog(key1 + "|" + key2 + "|" + key3, averageValueStr);
		NagiosUtils.addNagiosLog(StringUtil.join(fields, "|"), averageValueStr);
	}
}

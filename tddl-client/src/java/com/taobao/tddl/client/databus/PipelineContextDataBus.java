package com.taobao.tddl.client.databus;

import java.util.HashMap;
import java.util.Map;

import com.taobao.tddl.common.channel.SqlMetaData;

/**
 * �������ߣ���Ե�����ѯ�������ݸ��£����Ա���û�ж��߳�����
 * �����ҪӦ�ö��̳߳�������Ҫ�޸ĳɼ����Ĵ�ȡ��
 * 
 * @author junyu
 * @param <T>
 * 
 */
public class PipelineContextDataBus implements DataBus{
	private Map<String, Object> pluginContextMap = new HashMap<String, Object>();
	
	private SqlMetaData sqlMetaData;

	public Object getPluginContext(String name) {
		if (name == null) {
			return null;
		}
		return pluginContextMap.get(name);
	}
	
	public void registerPluginContext(String name, Object context) {
		if (name == null) {
			throw new NullPointerException("no plugin context name");
		}

		if (context == null) {
			throw new NullPointerException("no plugin context Object");
		}

		if (checkDuplicateName(name)) {
			throw new IllegalArgumentException("duplicated context name");
		}

		pluginContextMap.put(name, context);
	}

	public void removePluginContext(String name) {
		if (name == null) {
			throw new NullPointerException("no plugin context name");
		}

		pluginContextMap.remove(name);
	}
	
	@Override
	public SqlMetaData getSqlMetaData() {
		return sqlMetaData;
	}
	
	public void setSqlMetaData(SqlMetaData sqlMetaData){
		this.sqlMetaData = sqlMetaData;
	}

	private boolean checkDuplicateName(String name) {
		if (this.pluginContextMap.containsKey(name)) {
			return true;
		}
		return false;
	}
}
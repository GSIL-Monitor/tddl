//Copyrigh(c) Taobao.com
package com.taobao.tddl.common.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc ��ȡ���õĴ�����
 */
public interface ConfigDataHandler {
	public static final String FIRST_SERVER_STRATEGY = "firstServer";
	public static final String FIRST_CACHE_THEN_SERVER_STRATEGY="firstCache";

	/**
	 * DefaultConfigDataHandler���� ʵ���������Handler֮����ô˷��� ����Handler�����Ϣ
	 * @param dataId             ����������ƽ̨��ע���id
	 * @param listenerList       ���ݼ������б�
	 * @param prop               ȫ�����ú�����ʱ
	 */
	void init(String dataId, List<ConfigDataListener> listenerList,
			Map<String, Object> prop,final String unitName);
	
	void init(final String dataId,
			final List<ConfigDataListener> configDataListenerList,
			final Map<String, Object> config, final String unitName,
			String initialData);
	
	/**
	 * ������������ȡ����
	 * @param timeout    ��ȡ������Ϣ��ʱʱ��
	 * @param strategy   ��ȡ���ò���
	 * @return 
	 */
	String getData(long timeout, String strategy);
	
	/**
	 * ������������ȡ����
	 * @param timeout    ��ȡ������Ϣ��ʱʱ��
	 * @param strategy   ��ȡ���ò���
	 * @return 
	 */
	String getNullableData(long timeout, String strategy);

	/**
	 * Ϊ���͹���������ע�ᴦ��ļ�����
	 * @param configDataListener    ������
	 * @param executor              ִ�е�executor
	 */
	void addListener(ConfigDataListener configDataListener, Executor executor);

	/**
	 * Ϊ���͹���������ע�������������
	 * @param configDataListenerList  �������б�
	 * @param executor                ִ�е�executor
	 */
	void addListeners(List<ConfigDataListener> configDataListenerList,
			Executor executor);

	/**
	 * ֹͣ�ײ����ù�����
	 */
	void closeUnderManager();
}

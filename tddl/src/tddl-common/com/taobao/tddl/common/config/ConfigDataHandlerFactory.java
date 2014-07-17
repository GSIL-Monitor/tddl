package com.taobao.tddl.common.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author whisper
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc �õ���������ô�����ʵ��
 */
public interface ConfigDataHandlerFactory {
	/**
	 * ��ĳһ��dataId���м���
	 * @param dataId   ��������������ע���id
	 * @return         �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandler(String dataId,String unitName);
	
	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص�������
	 * @param dataId                ������p��ֵ����ע���id
	 * @param configDataListener    ���ݻص�������
	 * @return                      �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithListener(String dataId,
			ConfigDataListener configDataListener,String unitName);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������б�
	 * �����ṩִ���̳߳غ��ڲ�һЩ����(���ܱ�handler����)
	 * @param dataId                  ��������������ע���id
	 * @param configDataListenerList  ���ݻص��������б�
	 * @param executor                ���ݽ��մ����̳߳�
	 * @param config                  TDDL�ڲ���handler�ṩ��һЩ����
	 * @return                        �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithFullConfig(String dataId,
			List<ConfigDataListener> configDataListenerList, Executor executor,
			Map<String, String> config,String unitName);
}

package com.taobao.tddl.common.eagleeye;

import com.taobao.eagleeye.EagleEye;
import com.taobao.tddl.common.channel.SqlMetaData;

/**
 * @author jiechen.qzm
 * Eagleeye�����࣬Э����¼��ѯʱ��
 */
public class EagleeyeHelper {
	
	/**
	 * execute֮ǰд��־
	 * @param datasourceWrapper
	 * @param sqlType
	 * @throws Exception
	 */
	public static void startRpc(String ip, String port, String dbName, String sqlType){
		EagleEye.startRpc(dbName, sqlType);
		EagleEye.remoteIp(ip + ':' + port);
		EagleEye.rpcClientSend();
	}
	
	/**
	 * execute�ɹ�֮��д��־
	 */
	public static void endSuccessRpc(String sql){
		EagleEye.rpcClientRecv(EagleEye.RPC_RESULT_SUCCESS, EagleEye.TYPE_TDDL, EagleEye.index(sql));
	}
	
	/**
	 * executeʧ��֮��д��־
	 */
	public static void endFailedRpc(String sql){
		EagleEye.rpcClientRecv(EagleEye.RPC_RESULT_FAILED, EagleEye.TYPE_TDDL, EagleEye.index(sql));
	}

	/**
	 * @param sqlMetaData
	 * @param e
	 */
	public static void endRpc(SqlMetaData sqlMetaData, Exception e){
		if(e == null){
			endSuccessRpc(sqlMetaData.getLogicSql());
		}
		else {
			endFailedRpc(sqlMetaData.getLogicSql());
		}
	}
	
	
}

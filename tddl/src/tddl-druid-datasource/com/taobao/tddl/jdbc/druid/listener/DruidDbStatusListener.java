package com.taobao.tddl.jdbc.druid.listener;

import com.taobao.tddl.common.standard.atom.AtomDbStatusEnum;

/**���ݿ�״̬�仯������
 * 
 * @author qihao
 *
 */
public interface DruidDbStatusListener {

	void handleData(AtomDbStatusEnum oldStatus, AtomDbStatusEnum newStatus);
}

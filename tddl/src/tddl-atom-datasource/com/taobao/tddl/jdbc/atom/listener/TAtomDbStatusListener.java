package com.taobao.tddl.jdbc.atom.listener;

import com.taobao.tddl.common.standard.atom.AtomDbStatusEnum;

/**���ݿ�״̬�仯������
 * 
 * @author qihao
 *
 */
public interface TAtomDbStatusListener {

	void handleData(AtomDbStatusEnum oldStatus, AtomDbStatusEnum newStatus);
}

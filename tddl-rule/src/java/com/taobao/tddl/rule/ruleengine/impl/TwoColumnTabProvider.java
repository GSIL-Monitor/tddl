/**
 * 
 */
package com.taobao.tddl.rule.ruleengine.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.common.exception.checked.TDLCheckedExcption;
import com.taobao.tddl.common.sequence.Config;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.rule.ruleengine.TableRuleProvider;
import com.taobao.tddl.rule.ruleengine.TableRuleProviderRegister;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.TabRule;

/**
 * ֧��2���ֶ�ȡ����
 * 
 * @author liang
 * 
 */
public class TwoColumnTabProvider extends CommonTableRuleProvider {

	@Override
	public Set<String> getTables(Map<String, Comparative> map, TabRule tab,
			String tabName, Config config) throws TDLCheckedExcption {

		validTabRule(tab);

		String[] parameter = tab.getAllParameter();
		if (parameter.length != 2) {
			throw new TDLCheckedExcption("TwoColumnTabProvider�ֶα�����2������ǰ�ǣ�"
					+ tab.getParameter() + ".");
		}
		// ȡ������provider

		String expFunction = tab.getExpFunction().substring(
				tab.getExpFunction().indexOf('_') + 1);
		String[] provider = expFunction.split("\\|");

		if (provider.length != 2) {
			throw new TDLCheckedExcption("TwoColumnTabProvider����ʽ������2������ǰ�ǣ�"
					+ tab.getExpFunction() + ".");
		}

		TableRuleProvider providerA = null;
		TableRuleProvider providerB = null;
		Set<String> idSetA = null;
		Set<String> idSetB = null;
		Set<String> tabNameRes = new HashSet<String>();
		providerA = TableRuleProviderRegister
				.getTableRuleProviderByKey(provider[0]);
		providerB = TableRuleProviderRegister
				.getTableRuleProviderByKey(provider[1]);
		
		if (providerA != null) {
			TabRule tabRuleA = new TabRule();
			tabRuleA.setOffset(tab.getOffset());
			tabRuleA.setWidth(String.valueOf(tab.getWidth()));
			tabRuleA.setPadding(tab.getPadding());
			tabRuleA.setPrimaryKey(tab.getPrimaryKey());
			tabRuleA.setTableType(tab.getTableType());
			tabRuleA.setParameter(parameter[0]);
			tabRuleA.setExpFunction(provider[0]);
			tabRuleA.setDefaultTable(null);
			idSetA = providerA.getTables(map, tabRuleA, "", null);
		}
		if (providerB != null) {
			TabRule tabRuleB = new TabRule();
			tabRuleB.setOffset(tab.getOffset());
			tabRuleB.setWidth(String.valueOf(tab.getWidth()));
			tabRuleB.setPadding(tab.getPadding());
			tabRuleB.setPrimaryKey(tab.getPrimaryKey());
			tabRuleB.setTableType(tab.getTableType());
			tabRuleB.setParameter(parameter[1]);
			tabRuleB.setExpFunction(provider[1]);
			tabRuleB.setDefaultTable(null);
			idSetB = providerB.getTables(map, tabRuleB, "", null);
		}

		// TODO: ����Ҫ֧��һ��������������һ����Ĭ��ֵ�ϲ���Ŀǰ��֧��
		if (null == idSetA || null == idSetB) {
			return Collections.emptySet();
		}

		// ���ϲ�
		for (String one : idSetA) {
			for (String two : idSetB) {
				// ����Ƿ���Ĭ�ϱ���
				String tmp = tabName + one + two;
				if (tab.containThisTable(tmp)) {
					tabNameRes.add(tmp);
				}
			}
		}

		return tabNameRes;
	}
}
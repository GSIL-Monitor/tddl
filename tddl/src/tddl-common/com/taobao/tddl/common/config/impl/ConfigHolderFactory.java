package com.taobao.tddl.common.config.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.taobao.tddl.common.config.ConfigDataHolder;

public class ConfigHolderFactory {
	
	private static Map<String, ConfigDataHolder> holderMap = new ConcurrentHashMap<String, ConfigDataHolder>();

	public static ConfigDataHolder getConfigDataHolder(String appName) {
		return holderMap.get(appName);
	}

	public static void addConfigDataHolder(String appName, ConfigDataHolder configDataHolder) {
		holderMap.put(appName, configDataHolder);
	}
	
	public static void removeConfigHoder(String appName){
		holderMap.remove(appName);
	}
	
	public static boolean isInit(String appName){
		return holderMap.containsKey(appName);
	}

}

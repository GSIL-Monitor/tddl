package com.taobao.tddl.common.config;

import java.util.List;
import java.util.Map;

public interface ConfigDataHolder {
	
	public String getData(String dataId);
	
	public Map<String, String> getData(List<String> dataIds);
	
}

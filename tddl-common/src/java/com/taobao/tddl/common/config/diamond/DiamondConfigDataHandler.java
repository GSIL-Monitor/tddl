package com.taobao.tddl.common.config.diamond;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.impl.DiamondEnv;
import com.taobao.diamond.client.impl.DiamondEnvRepo;
import com.taobao.diamond.client.impl.DiamondUnitSite;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.SkipInitialCallbackListener;
import com.taobao.tddl.common.config.ConfigDataHandler;
import com.taobao.tddl.common.config.ConfigDataListener;
import com.taobao.tddl.common.util.TDDLMBeanServer;
import com.taobao.tddl.common.util.mbean.TDDLMBean;

/**
 * @author shenxun
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc �־���������diamondʵ��
 */
public class DiamondConfigDataHandler implements ConfigDataHandler {
	private static final Log logger = LogFactory
			.getLog(DiamondConfigDataHandler.class);
	public static final long TIMEOUT = 10 * 1000;
	protected String dataId;
	protected String unitName;
	private String mbeanId;
	private TDDLMBean mbean;
	private DiamondEnv env;

	public void init(final String dataId,
			final List<ConfigDataListener> configDataListenerList,
			final Map<String, Object> config, final String unitName) {
		this.init(dataId, configDataListenerList, config, unitName, null);
	}
	
	public void init(final String dataId,
			final List<ConfigDataListener> configDataListenerList,
			final Map<String, Object> config, final String unitName,String initialData) {
		mbean = new TDDLMBean("Diamond Config Info "
				+ System.currentTimeMillis());
		mbeanId = dataId + System.currentTimeMillis();
		if (unitName != null && !"".equals(unitName.trim())) {
			env = DiamondUnitSite.getDiamondUnitEnv(unitName);
		} else {
			env = DiamondEnvRepo.defaultEnv;
		}

		if (initialData == null) {
			try {
				initialData = env.getConfig(dataId, null,
						Constants.GETCONFIG_LOCAL_SERVER_SNAPSHOT, TIMEOUT);
			} catch (IOException e) {
				throw new RuntimeException("get diamond data error!dataId:"
						+ dataId, e);
			}
		}

		env.addListeners(dataId, null,
				Arrays.asList(new SkipInitialCallbackListener(initialData) {
					@Override
					public Executor getExecutor() {
						return (Executor) config.get("executor");
					}

					@Override
					public void receiveConfigInfo0(String arg0) {
						if (configDataListenerList != null) {
							for (ConfigDataListener configDataListener : configDataListenerList) {
								configDataListener.onDataRecieved(dataId, arg0);
							}

							if (arg0 != null) {
								mbean.setAttribute(dataId, arg0);
							} else {
								mbean.setAttribute(dataId, "");
							}
						}
					}
				}));

		this.unitName = unitName;
		this.dataId = dataId;
		TDDLMBeanServer.registerMBeanWithId(mbean, mbeanId);
	}

	public String getNullableData(long timeout, String strategy) {
		return getData(timeout, strategy);
	}
	
	public String getData(long timeout, String strategy) {
		String data = null;
		try {
			data = env.getConfig(dataId, null,
					Constants.GETCONFIG_LOCAL_SERVER_SNAPSHOT, timeout);
		} catch (IOException e) {
			throw new RuntimeException("get diamond data error!dataId:"
					+ dataId, e);
		}

		if (data != null) {
			mbean.setAttribute(dataId, data);
		} else {
			mbean.setAttribute(dataId, "");
		}

		return data;
	}
	
	public void addListener(final ConfigDataListener configDataListener,
			final Executor executor) {
		if (configDataListener != null) {
			String data = null;
			try {
				data = env.getConfig(dataId, null,
						Constants.GETCONFIG_LOCAL_SERVER_SNAPSHOT, TIMEOUT);
			} catch (IOException e) {
				throw new RuntimeException("get diamond data error!dataId:"
						+ dataId, e);
			}

			env.addListeners(dataId, null,
					Arrays.asList(new SkipInitialCallbackListener(data) {
						@Override
						public Executor getExecutor() {
							return executor;
						}

						@Override
						public void receiveConfigInfo0(String arg0) {
							configDataListener.onDataRecieved(dataId, arg0);
							if (arg0 != null) {
								mbean.setAttribute(dataId, arg0);
							} else {
								mbean.setAttribute(dataId, "");
							}
						}
					}));
		}
	}

	public void addListeners(
			final List<ConfigDataListener> configDataListenerList,
			final Executor executor) {
		if (configDataListenerList != null) {
			String data = null;
			try {
				data = env.getConfig(dataId, null,
						Constants.GETCONFIG_LOCAL_SERVER_SNAPSHOT, TIMEOUT);
			} catch (IOException e) {
				throw new RuntimeException("get diamond data error!dataId:"
						+ dataId, e);
			}

			env.addListeners(dataId, null,
					Arrays.asList(new SkipInitialCallbackListener(data) {
						@Override
						public Executor getExecutor() {
							return executor;
						}

						@Override
						public void receiveConfigInfo0(String arg0) {
							for (ConfigDataListener configDataListener : configDataListenerList) {
								try {
									configDataListener.onDataRecieved(dataId,
											arg0);
								} catch (Exception e) {
									logger.error("one of listener failed", e);
									continue;
								}
							}

							if (arg0 != null) {
								mbean.setAttribute(dataId, arg0);
							} else {
								mbean.setAttribute(dataId, "");
							}
						}
					}));
		}
	}

	public void closeUnderManager() {
		List<ManagerListener> listeners = env.getListeners(dataId, null);
		for (ManagerListener l : listeners) {
			env.removeListener(dataId, null, l);
		}
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
}

package com.taobao.tddl.common.monitor.stat;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * ���ڴ���ܹ��ܵ���־������ߡ������ TPS ����ͳ����־��̫������⡣ <br />
 * 
 * ���ߵĹ�������� Key ��ͬ��ͳ�Ƽ�¼, ���ܼ���� sum/min/max, �ٶ�ʱˢ������־�� <br />
 * 
 * Ŀǰ������������ BufferedLogWriter �� flushAll �����������־����, ��Ϊ��Щ���������ڳ��Ѿ����� Old Gen,
 * ��ʱ�����л���� Old Gen �������� Full GC. <br />
 * 
 * ʹ�÷����ǣ�
 * 
 * <pre>
 * BufferStatLogWriter.write(
 *     new Object[] { key1, key2, key3, ... },  // ͳ�Ƶ�Ŀ�� 
 *     count, value);                           // ͳ��ֵ
 * 
 * BufferStatLogWriter.write(
 *     new Object[] { key1, key2, key3, ... },  // ͳ�Ƶ�Ŀ�� 
 *     new Object[] { obj1, obj2, obj3, ... },  // ���ӵĶ���
 *     count, value);                           // ͳ��ֵ
 * </pre>
 * 
 * @author changyuan.lh
 */
public class BufferedLogWriter extends AbstractStatLogWriter {

	protected static final Logger logger = Logger.getLogger(BufferedLogWriter.class);

	public volatile int flushInterval = 300; // ��λ�롣Ĭ�� 5 ����ȫ��ˢ��һ��

	protected volatile int minKeySize = 1024;
	public volatile int maxKeySize = 65536;

	protected volatile ConcurrentHashMap<LogKey, LogCounter> map;

	protected final StatLogWriter nestLog;

	public BufferedLogWriter(int flushInterval, int minKeySize, int maxKeySize,
			StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, LogCounter>( // NL
				minKeySize, 0.75f, 32);
		this.flushInterval = flushInterval;
		this.minKeySize = minKeySize;
		this.maxKeySize = maxKeySize;
		this.nestLog = nestLog;
		schdeuleFlush();
	}

	public BufferedLogWriter(int minKeySize, int maxKeySize,
			StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, LogCounter>( // NL
				minKeySize, 0.75f, 32);
		this.minKeySize = minKeySize;
		this.maxKeySize = maxKeySize;
		this.nestLog = nestLog;
		schdeuleFlush();
	}

	public BufferedLogWriter(StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, LogCounter>( // NL
				minKeySize, 0.75f, 32);
		this.nestLog = nestLog;
		schdeuleFlush();
	}

	public void setMinKeySize(int minKeySize) {
		this.minKeySize = minKeySize;
	}

	public int getMinKeySize() {
		return minKeySize;
	}

	public void setMaxKeySize(int maxKeySize) {
		this.maxKeySize = maxKeySize;
	}

	public int getMaxKeySize() {
		return maxKeySize;
	}

	public void setFlushInterval(int flushInterval) {
		if (this.flushInterval != flushInterval) {
			this.flushInterval = flushInterval;
			schdeuleFlush();
		}
	}

	public int getFlushInterval() {
		return flushInterval;
	}

	/**
	 * ���ڴ��л���ͳ����Ϣ, ����������� maxKeySize ����, �첽ִ�� flushLRU().
	 */
	public void write(Object[] keys, Object[] fields, long... values) {
		if (values.length != 2) {
			// XXX: �������� BufferedLogWriter ֻ���� count + value ������
			throw new IllegalArgumentException("Only support 2 values!");
		}
		ConcurrentHashMap<LogKey, LogCounter> map = this.map;
		LogKey logKey = new LogKey(keys);
		LogCounter counter = map.get(logKey);
		if (counter == null) {
			LogCounter newCounter = new LogCounter(logKey,
					(fields == null) ? keys : fields);
			newCounter.stat(values[0], values[1]);
			counter = map.putIfAbsent(logKey, newCounter);
			if (counter == null) {
				insureMaxSize();
				return;
			}
		}
		counter.stat(values[0], values[1]);
	}

	protected void insureMaxSize() {
		if (map.size() > maxKeySize) {
			flush(false);
		}
	}

	protected volatile TimerTask flushTask = null;

	protected final Lock flushLock = new ReentrantLock();

	protected volatile boolean flushing = false;

	public boolean flush(final boolean flushAll) {
		if (!flushing && flushLock.tryLock()) {
			try {
				flushing = true;
				flushExecutor.execute(new Runnable() {
					public void run() {
						try {
							if (flushAll) {
								flushAll();
							} else {
								flushLRU();
							}
						} finally {
							flushing = false;
						}
					}
				});
			} finally {
				flushLock.unlock();
			}
			return true;
		}
		return false;
	}

	private final synchronized void schdeuleFlush() {
		TimerTask cancelTask = this.flushTask;
		this.flushTask = new TimerTask() {
			public void run() {
				// XXX: ��ʱ����ִ��Ӧ����ʱ�ǳ���
				flush(true);
			}
		};
		if (cancelTask != null) {
			cancelTask.cancel();
		}
		final long flushPriod = flushInterval * 1000;
		flushTimer.scheduleAtFixedRate(flushTask, flushPriod, flushPriod);
	}

	private final int flushLog(Map<LogKey, LogCounter> logs) {
		int count = 0;
		for (Entry<LogKey, LogCounter> entry : logs.entrySet()) {
			LogCounter counter = entry.getValue();
			nestLog.write(entry.getKey().getKeys(), counter.getFields(),
					counter.getValues());
			count++;
		}
		return count;
	}

	private final void flushLog(LogKey logKey, LogCounter counter) {
		nestLog.write(logKey.getKeys(), counter.getFields(),
				counter.getValues());
	}

	/**
	 * ˢ�����е���־ͳ����Ϣ��
	 */
	protected void flushAll() {
		final long flushMillis = System.currentTimeMillis();
		final int initKeySize = Math
				.max(minKeySize, (int) (map.size() / 0.75f));
		Map<LogKey, LogCounter> map = this.map;
		this.map = new ConcurrentHashMap<LogKey, LogCounter>( // NL
				initKeySize, 0.75f, 32);
		// �ȴ�������Ӽ�¼���߳�ִ�����
		LockSupport.parkNanos(5000);
		// XXX: �������־�� Key �������� -- ��ȡ��
		// map = new TreeMap<LogKey, LogCounter>(map);
		int count = flushLog(map);
		if (count > 0 && logger.isDebugEnabled()) {
			logger.debug("flushAll: " + map.size() + " logs in "
					+ (System.currentTimeMillis() - flushMillis)
					+ " milliseconds.");
		}
	}

	/**
	 * ˢ��ͳ�ƴ������ٵ���־��Ϣ, ֻ���� maxKeySize ������ 2/3.
	 */
	protected void flushLRU() {
		final long flushMillis = System.currentTimeMillis();
		// XXX: �������־�� Key �������� -- ��ȡ��
		// Map<LogKey, LogCounter> flushLogs = new TreeMap<LogKey,
		// LogCounter>();
		Map<LogKey, LogCounter> map = this.map;
		int keep = maxKeySize * 2 / 3; // ���� 2/3
		int flush = map.size() - keep; // ��ʱ size �����Ѿ�����
		int count = 0;
		// changyuan.lh: �������� count = 1 �ļ�¼, ����Ҫ�����ڴ��ٶȱȽϿ�
		for (Entry<LogKey, LogCounter> entry : map.entrySet()) {
			if (flush <= 0)
				break;
			LogKey logKey = entry.getKey();
			LogCounter counter = entry.getValue();
			if (counter.getCount() < 2) {
				LogCounter removed = map.remove(logKey);
				if (removed != null) {
					// flushLogs.put(logKey, counter);
					flushLog(logKey, removed);
					flush--;
					count++;
				}
			}
		}
		// changyuan.lh: Ȼ�� LRU ����ȫ���ļ�¼, ��Ҫ��������, �ڴ�ռ�ñȽϸ�
		flush = map.size() - keep; // ��ʱ size �����Ѿ�����
		if (flush > 0) {
			Object[] counters = map.values().toArray();
			Arrays.sort(counters);
			for (int i = 0; i < Math.min(flush, counters.length); i++) {
				LogCounter counter = (LogCounter) counters[i];
				LogKey logKey = counter.getLogKey();
				LogCounter removed = map.remove(logKey);
				if (removed != null) {
					// flushLogs.put(logKey, removed);
					flushLog(logKey, counter);
					count++;
				}
			}
		}
		// flushLog(flushLogs);
		if (count > 0 && logger.isDebugEnabled()) {
			logger.debug("flushLRU: " + count + " logs in "
					+ (System.currentTimeMillis() - flushMillis)
					+ " milliseconds.");
		}
	}
}

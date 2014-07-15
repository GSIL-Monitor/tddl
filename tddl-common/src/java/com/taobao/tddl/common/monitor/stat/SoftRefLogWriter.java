package com.taobao.tddl.common.monitor.stat;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * ���ڴ���ܹ��ܵ���־������ߵ���һ��ʵ��, ʹ�� SoftReference/WeakReferece ��� BufferedLogWriter
 * OldGen �ڴ�ռ�ú� FullGC�����⡣ <br />
 * 
 * ������ߵķ����� SoftReference ������ʱ����ȫ�� JVM ȷ��, �ڴ󲿷�����¶�������� SoftReference �����ڴ治��,
 * ��������ɲ���Ҫ���ڴ�ռ�á� <br />
 * 
 * ���ʹ�� WeakReference �ķ�����Ƶ���� minor GC �����ͳ�Ƽ�¼, ��ɴ���ͳ�Ƽ�¼�Ķ�ʧ�� <br />
 * 
 * ��˽����ʹ�÷����ǣ�
 * 
 * <pre>
 * // ���� counter ����, ���ұ������ڴ�
 * LogCounter counter = SoftRefLogWriter.getCounter(
 *     new Object[] { key1, key2, key3, ... });  // ͳ�Ƶ�Ŀ�� 
 * 
 * // ���� counter ����, ���ұ������ڴ�
 * LogCounter counter = SoftRefLogWriter.getCounter(
 *     new Object[] { key1, key2, key3, ... },   // ͳ�Ƶ�Ŀ�� 
 *     new Object[] { obj1, obj2, obj3, ... });  // ���ӵĶ���
 * 
 * counter.stat(count, value);  // ���ͳ��ֵ
 * </pre>
 * 
 * @author changyuan.lh
 */
public class SoftRefLogWriter extends AbstractStatLogWriter {

	protected static final Logger logger = Logger.getLogger(BufferedLogWriter.class);

	protected volatile int flushInterval = 300; // ��λ�롣Ĭ�� 5 ����ȫ��ˢ��һ��

	protected volatile boolean softRef = true;

	protected static final class WeakRefLogCounter extends
			WeakReference<LogCounter> {
		final LogKey logKey;

		public WeakRefLogCounter(LogKey logKey, LogCounter counter,
				ReferenceQueue<LogCounter> queue) {
			super(counter, queue);
			this.logKey = logKey;
		}
	}

	protected static final class SoftRefLogCounter extends
			SoftReference<LogCounter> {
		final LogKey logKey;

		public SoftRefLogCounter(LogKey logKey, LogCounter counter,
				ReferenceQueue<LogCounter> queue) {
			super(counter, queue);
			this.logKey = logKey;
		}
	}

	protected volatile ConcurrentHashMap<LogKey, Reference<LogCounter>> map;

	/**
	 * Reference queue for cleared WeakEntries
	 */
	private final ReferenceQueue<LogCounter> queue = new ReferenceQueue<LogCounter>();

	protected final StatLogWriter nestLog;

	public SoftRefLogWriter(boolean softRef, int flushInterval,
			StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, Reference<LogCounter>>( // NL
				1024, 0.75f, 32);
		this.flushInterval = flushInterval;
		this.softRef = softRef;
		this.nestLog = nestLog;
		schdeuleFlush();
	}

	public SoftRefLogWriter(boolean softRef, StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, Reference<LogCounter>>( // NL
				1024, 0.75f, 32);
		this.softRef = softRef;
		this.nestLog = nestLog;
		schdeuleFlush();
	}

	public SoftRefLogWriter(StatLogWriter nestLog) {
		this.map = new ConcurrentHashMap<LogKey, Reference<LogCounter>>( // NL
				1024, 0.75f, 32);
		this.nestLog = nestLog;
		schdeuleFlush();
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

	public boolean isSoftRef() {
		return softRef;
	}

	public void setSoftRef(boolean softRef) {
		this.softRef = softRef;
	}

	/**
	 * ������¼����� SoftReference/WeakReference.
	 */
	protected final Reference<LogCounter> createLogRef(LogKey logKey,
			LogCounter counter) {
		return softRef ? new SoftRefLogCounter(logKey, counter, queue)
				: new WeakRefLogCounter(logKey, counter, queue);
	}

	/**
	 * �����Ѿ����յĶ���ռ�ݵ� map entry.
	 */
	protected final void expungeLogRef() {
		final long expungeMillis = System.currentTimeMillis();
		int count = 0;
		Reference<? extends LogCounter> entry;
		while ((entry = queue.poll()) != null) {
			if (entry instanceof SoftRefLogCounter) {
				map.remove(((SoftRefLogCounter) entry).logKey);
				count++;
			} else if (entry instanceof WeakRefLogCounter) {
				map.remove(((WeakRefLogCounter) entry).logKey);
				count++;
			}
		}
		if (count > 0 && logger.isDebugEnabled()) {
			logger.debug("expungeLogRef: " + count + " logs in "
					+ (System.currentTimeMillis() - expungeMillis)
					+ " milliseconds.");
		}
	}

	/**
	 * ����һ����¼����, ���߷��ػ��������еļ�¼��
	 */
	public LogCounter getCounter(Object[] keys, Object[] fields) {
		ConcurrentHashMap<LogKey, Reference<LogCounter>> map = this.map;
		LogKey logKey = new LogKey(keys);
		LogCounter counter;
		for (;;) {
			Reference<LogCounter> entry = map.get(logKey);
			if (entry == null) {
				LogCounter newCounter = new LogCounter(logKey,
						(fields == null) ? keys : fields);
				entry = map.putIfAbsent(logKey,
						createLogRef(logKey, newCounter));
				if (entry == null) {
					expungeLogRef();
					return newCounter;
				}
			}
			counter = entry.get();
			if (counter != null) {
				return counter;
			}
			map.remove(logKey);
		}
	}

	/**
	 * ���ڴ��л���ͳ����Ϣ��
	 */
	public void write(Object[] keys, Object[] fields, long... values) {
		if (values.length != 2) {
			// XXX: �������� BufferedLogWriter ֻ���� count + value ������
			throw new IllegalArgumentException("Only support 2 values!");
		}
		ConcurrentHashMap<LogKey, Reference<LogCounter>> map = this.map;
		LogKey logKey = new LogKey(keys);
		LogCounter counter;
		for (;;) {
			Reference<LogCounter> entry = map.get(logKey);
			if (entry == null) {
				LogCounter newCounter = new LogCounter(logKey,
						(fields == null) ? keys : fields);
				newCounter.stat(values[0], values[1]);
				entry = map.putIfAbsent(logKey,
						createLogRef(logKey, newCounter));
				if (entry == null) {
					expungeLogRef();
					return;
				}
			}
			counter = entry.get();
			if (counter != null) {
				counter.stat(values[0], values[1]);
				return;
			}
			map.remove(logKey);
		}
	}

	protected volatile TimerTask flushTask = null;

	protected final Lock flushLock = new ReentrantLock();

	protected volatile boolean flushing = false;

	public boolean flush() {
		if (!flushing && flushLock.tryLock()) {
			try {
				flushing = true;
				flushExecutor.execute(new Runnable() {
					public void run() {
						try {
							flushAll();
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
				flush();
			}
		};
		if (cancelTask != null) {
			cancelTask.cancel();
		}
		final long flushPriod = flushInterval * 1000;
		flushTimer.scheduleAtFixedRate(flushTask, flushPriod, flushPriod);
	}

	/**
	 * ˢ�����е���־ͳ����Ϣ��
	 */
	protected void flushAll() {
		final long flushMillis = System.currentTimeMillis();
		// �����Ѿ����յĶ���
		expungeLogRef();
		// XXX: �������־�� Key �������� -- ��ȡ��
		// TreeMap<LogKey, Reference<LogCounter>> map = new TreeMap<LogKey,
		// SoftReference<LogCounter>>(map);
		ConcurrentHashMap<LogKey, Reference<LogCounter>> map = this.map;
		int count = 0;
		for (Entry<LogKey, Reference<LogCounter>> entry : map.entrySet()) {
			LogCounter counter = entry.getValue().get();
			if (counter != null && counter.getCount() > 0) {
				LogKey logKey = entry.getKey();
				nestLog.write(logKey.getKeys(), counter.getFields(),
						counter.getValues());
				counter.clear();
				count++;
			}
		}
		if (count > 0 && logger.isDebugEnabled()) {
			logger.debug("flushAll: " + count + " logs in "
					+ (System.currentTimeMillis() - flushMillis)
					+ " milliseconds.");
		}
	}
}

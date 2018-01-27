package org.pillar.lru;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * General-purpose LRU cache
 *
 * @param <K> key
 * @param <V> value
 */
public class LRUCache<K, V> {
	protected Map<K, V> lruData;
	protected final ReentrantLock lock;

	public LRUCache() {
		lock = new ReentrantLock();
	}

	public LRUCache(ReentrantLock lock) {
		this.lock = lock;
	}

	/**
	 * Set cache capacity
	 * @param capacity capacity
	 */
	public void init(final int capacity) {
		lruData = new LinkedHashMap<K, V>(capacity + 1, 1.0f, true) {
			private static final long serialVersionUID = 7293575125752194475L;

			protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
				return (this.size() > capacity);
			};
		};
	}
	
	/**
	 * Returns the value to which the specified key is mapped.
	 * If the value exists - updates it's status in expiration queue
	 * 
	 * @param key key
	 * @return value value
	 */
	public V tryGet(K key) {
		lock.lock();
		try {
			V value = lruData.get(key);
			if (value != null) {
				lruData.remove(key);
				lruData.put(key, value);
			}
			return value;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Associates the specified value with the specified key in this map
	 * 
	 * @param key key
	 * @param value value
	 */
	public void put(K key, V value) {
		lock.lock();
		try {
			lruData.remove(key);
			lruData.put(key, value);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes the mapping for a key from this map if it is present
	 * 
	 * @param key key
	 */
	public void remove(K key) {
		lock.lock();
		try {
			lruData.remove(key);
		} finally {
			lock.unlock();
		}
	}
}
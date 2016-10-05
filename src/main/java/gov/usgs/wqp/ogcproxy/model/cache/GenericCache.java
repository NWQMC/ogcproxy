package gov.usgs.wqp.ogcproxy.model.cache;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

// Based on http://www.javacreed.com/how-to-cache-results-to-boost-performance/
public class GenericCache<K, V> {

	private final ConcurrentMap<K, Future<V>> cache = new ConcurrentHashMap<>();

	private Future<V> createFutureIfAbsent(final K key, final Callable<V> callable) {
		Future<V> future = cache.get(key);
		if (future == null) {
			final FutureTask<V> futureTask = new FutureTask<V>(callable);
			future = cache.putIfAbsent(key, futureTask);
			if (future == null) {
				future = futureTask;
				futureTask.run();
			}
		}
		return future;
	}

	public V getValue(final K key, final Callable<V> callable) throws InterruptedException, ExecutionException {
		try {
			final Future<V> future = createFutureIfAbsent(key, callable);
			return future.get();
		} catch (final InterruptedException e) {
			cache.remove(key);
			throw e;
		} catch (final ExecutionException e) {
			cache.remove(key);
			throw e;
		} catch (final RuntimeException e) {
			cache.remove(key);
			throw e;
		}
	}

	public void setValueIfAbsent(final K key, final V value) {
		createFutureIfAbsent(key, new Callable<V>() {
			@Override
			public V call() throws Exception {
				return value;
			}
		});
	}

	public Collection<Future<V>> getValues() {
		final Collection<Future<V>> future = cache.values();
		return future;
	}

	public int size() {
		return cache.size();
	}

	public void clear() {
		cache.clear();
	}
}

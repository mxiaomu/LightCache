package com.example;

import com.github.benmanes.caffeine.cache.Cache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class CaffeineCache<V> implements ILightCache<V>{

		public Cache<? super String, CacheWrapper<?>> cache;
		private final String cacheName;
		private final long timeout;
		private final SyncStrategy strategy;

		public CaffeineCache(CacheManager cacheManager, SyncStrategy strategy, String cacheName) {
				this.cache = cacheManager.selectCaffeineCache( cacheName );
				this.timeout = cacheManager.getTimeoutSecs(cacheName);
				this.cacheName = cacheName;
				this.strategy = strategy;
		}

		@Override
		public V getIfPresent(@NonNull String key) {
				String cacheKey = generateKey(key);
				return Optional.ofNullable(cache.getIfPresent(cacheKey)).map(wrapper -> (V) wrapper.getValue() ).orElse(null);
		}

		@Override
		public V get(@NonNull String key, long timeoutSecs, @NonNull Function<? super String, ? extends V> mappingFunction) {
				String cacheKey = generateKey(key);
				return Optional.ofNullable(cache.get( cacheKey, (k) -> new CacheWrapper<>(timeoutSecs, mappingFunction.apply(key) )))
								.map(wrapper -> (V) wrapper.getValue())
								.orElse(null);
		}

		@Override
		public void put(@NonNull String key, @NonNull V value) {
				String cacheKey = generateKey(key);
				cache.put(cacheKey, new CacheWrapper<>(timeout, value));
				if ( strategy != null) {
						strategy.pushSyncEvent(cacheName, cacheKey);
				}
		}

		@Override
		public void put(@NonNull String key, @NonNull V value, long timeoutSecs) {
				String cacheKey = generateKey(key);
				cache.put(cacheKey, new CacheWrapper<>(timeoutSecs, value));
				if ( strategy != null ) {
						strategy.pushSyncEvent(cacheName, cacheKey);
				}
		}

		@Override
		public void putIfAbsent(@NonNull String key, @NonNull V value, long timeoutSecs) {
				get(key, timeoutSecs, (k) -> value);
		}

		@Override
		public void putIfAbsent(@NonNull String key, @NonNull V value) {
				get(key, timeout, (k) -> value);
		}

		@Override
		public void putAll(@NonNull Map<? extends String, ? extends V> map) {
				Map<String, CacheWrapper<V>> newMap = new HashMap<>();
				List<String> cacheKeys = new ArrayList<>();
				for (Map.Entry<? extends String, ? extends V> entry : map.entrySet() ) {
						String cacheKey = generateKey( entry.getKey() );
						cacheKeys.add( cacheKey );
						newMap.put(cacheKey, new CacheWrapper<>(timeout, entry.getValue()));
				}
				cache.putAll(newMap);
				if (strategy!=null) {
						strategy.pushSyncEvent(cacheName, cacheKeys.toArray(new String[0]));
				}
		}

		@Override
		public void invalidate(@NonNull String key) {
				String cacheKey = generateKey(key);
				cache.invalidate(cacheKey);
				if (strategy != null) {
						strategy.pushSyncEvent(cacheName, cacheKey);
				}
		}

		@Override
		public void invalidateAll(@NonNull Iterable<@NonNull ?> keys) {
				final Iterator<@NonNull ?> iterator = keys.iterator();
				List<String> keyCollection = new ArrayList<>();
				while (iterator.hasNext()){
						keyCollection.add(generateKey((String) iterator.next()));
				}
				cache.invalidateAll(keyCollection);
				if (strategy!=null) {
						strategy.pushSyncEvent(cacheName, "*");
				}
		}

		@Override
		public void cleanup() {
				cache.cleanUp();
				if (strategy!=null) {
						strategy.pushSyncEvent(cacheName, "*");
				}
		}

		public V searchData(String key, Function<? super String, ? extends V> mappingFunction) {
				final V content = getIfPresent(key);
				if ( content == null ) {
						return mappingFunction.apply(key);
				}
				return content;
		}

		private String generateKey(String key) {
				return cacheName + ":" + key;
		}

}

package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class GenericCache<V> extends AbsCacheAdapter<V> {

		public GenericCache(CaffeineCache<V> caffeineCache, RedisCache<V> redisCache) {
				super(caffeineCache, redisCache);
		}

		@Override
		public V loadCache(@NonNull String key, long timeout, @NonNull TimeUnit timeUnit, Function<? super String, ? extends V> mappingFunction) {
				final long timeoutSecs = timeUnit.toSeconds(timeout);
				return caffeineCache.get(key, timeoutSecs, (k1) -> redisCache.get(k1, timeoutSecs, mappingFunction));
		}

		@Override
		public V loadCacheIfAbsent(@NonNull String key) {
				return caffeineCache.searchData(key, redisCache::getIfPresent);
		}

		@Override
		public void removeCache(@NonNull String key) {
				redisCache.invalidate(key);
				caffeineCache.invalidate(key);
		}
}

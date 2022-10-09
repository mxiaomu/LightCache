package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class RedisCache<V> implements ILightCache<V> {

		private final RedisTemplate<String, V> redisTemplate;
		private final String cacheName;
		private final long timeout;

		public <K> RedisCache(CacheManager cacheManager, String cacheName) {
				this.cacheName = cacheName;
				this.redisTemplate = (RedisTemplate<String, V>) cacheManager.selectRedisCache();
				this.timeout = cacheManager.getTimeoutSecs(cacheName);
		}

		@Override
		public V getIfPresent(@NonNull String key) {
				return redisTemplate.opsForValue().get( key );
		}

		@Override
		public V get(@NonNull String key, long timeoutSecs, @NonNull Function<? super String, ? extends V> mappingFunction) {
				final V data0 = getIfPresent(key);
				if ( null == data0 ) {
						final V data1 = mappingFunction.apply(key);
						this.redisTemplate.opsForValue().set( key, data1, timeoutSecs, TimeUnit.SECONDS);
						return data1;
				}
				return data0;
		}


		@Override
		public void put(@NonNull String key, @NonNull V value) {
				this.redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
		}

		@Override
		public void put(@NonNull String key, @NonNull V value, long timeoutSecs) {
				this.redisTemplate.opsForValue().set(key, value, timeoutSecs, TimeUnit.SECONDS);
		}

		@Override
		public void putIfAbsent(@NonNull String key, @NonNull V value, long timeoutSecs) {
				this.redisTemplate.opsForValue().setIfAbsent(key, value, timeoutSecs, TimeUnit.SECONDS);
		}

		@Override
		public void putIfAbsent(@NonNull String key, @NonNull V value) {
				this.redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
		}

		@Override
		public void putAll(@NonNull Map<? extends @NonNull String, ? extends V> map) {
				this.redisTemplate.opsForValue().multiSet(map);
		}

		@Override
		public void invalidate(@NonNull String key) {
				this.redisTemplate.delete(key);
		}

		@Override
		public void invalidateAll(@NonNull Iterable<@NonNull ?> keys) {
				List<String> collections = new ArrayList<>();
				for (Object key : keys) {
						collections.add((String) key);
				}
				this.redisTemplate.delete( collections );
		}

		@Override
		public void cleanup() {
				final String pattern = cacheName + ":*";
				this.redisTemplate.delete( pattern );
		}
}

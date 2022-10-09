package com.example;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AbsCacheAdapter<V> {

		protected final CaffeineCache<V> caffeineCache;
		protected final RedisCache<V> redisCache;

		public AbsCacheAdapter(CaffeineCache<V> caffeineCache, RedisCache<V> redisCache){
				this.caffeineCache = caffeineCache;
				this.redisCache = redisCache;
		}
		public abstract V loadCache(@NonNull String key, long timeout, @NonNull TimeUnit timeUnit, Function<? super String, ? extends V> mappingFunction);
		public abstract V loadCacheIfAbsent(@NonNull String key);
		public abstract void removeCache(@NonNull String key);

}

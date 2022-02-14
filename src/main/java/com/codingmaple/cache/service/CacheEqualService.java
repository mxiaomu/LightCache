package com.codingmaple.cache.service;

import com.codingmaple.cache.constants.CacheEqual;

@FunctionalInterface
public interface CacheEqualService<T> {
		CacheEqual compare(T localCache, T redisCache);
}

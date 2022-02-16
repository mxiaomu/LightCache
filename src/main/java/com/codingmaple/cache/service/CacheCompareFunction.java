package com.codingmaple.cache.service;

import com.codingmaple.cache.constants.CacheState;

@FunctionalInterface
public interface CacheCompareFunction<T> {
		boolean compare(T localCache, T redisCache);
}

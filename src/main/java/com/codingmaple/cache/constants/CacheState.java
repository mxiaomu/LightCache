package com.codingmaple.cache.constants;

public enum CacheState {
		// 缓存一致，且相等
		OK,
		// 缓存不存在
		NOT_FOUND,
		// 没有一级缓存
		NOT_FOUND_LOCAL,
		// 没有二级缓存
		NOT_FOUND_REDIS;
}

package com.flyingbreeze.cache.core;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface ILightCache {

		<T> T loadFromTwoLevelCache(String cacheName, String key, long timeout, TimeUnit timeUnit, Supplier<T> provider);
		<T> T loadFromOneLevelCache(String cacheName, String key, Supplier<T> provider);
		void clearOneLevelCache(String cacheName, String key);
		void clearTwoLevelCache(String cacheName, String key);
}

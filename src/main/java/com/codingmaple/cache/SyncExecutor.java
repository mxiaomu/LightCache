package com.codingmaple.cache;

@FunctionalInterface
public interface SyncExecutor {
		void executeSync(CacheInfo cacheInfo);
}

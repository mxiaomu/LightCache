package com.codingmaple.cache.strategy.impl.raft;

public interface CacheRaftSyncService {
		public void syncCache(String cacheName, String cacheKey);
}

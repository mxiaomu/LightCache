package com.codingmaple.cache.stragety;

import com.codingmaple.cache.CacheInfo;
import com.codingmaple.cache.SyncExecutor;

public interface SyncCacheStrategy {

		public void publishSyncCacheEvent(CacheInfo cacheInfo);

		void subscribe (String cacheName, SyncExecutor syncExecutor);

}

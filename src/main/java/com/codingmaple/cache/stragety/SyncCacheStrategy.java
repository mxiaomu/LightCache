package com.codingmaple.cache.stragety;

import com.codingmaple.cache.CacheInfo;

public interface SyncCacheStrategy {

		public void publishSyncCacheEvent(CacheInfo cacheInfo);

		public void syncCache(CacheInfo cacheInfo);

}

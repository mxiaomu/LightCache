package com.codingmaple.cache;

import com.codingmaple.cache.constants.CacheStatus;

public class CacheProvider<T>{

		private T cache;
		private CacheStatus cacheStatus;

		public CacheProvider(T cache, CacheStatus cacheStatus){
				this.cache = cache;
				this.cacheStatus = cacheStatus;
		}

		public T getCache() {
				return cache;
		}

		public void setCache(T cache) {
				this.cache = cache;
		}

		public CacheStatus getCacheStatus() {
				return cacheStatus;
		}

		public void setCacheStatus(CacheStatus cacheStatus) {
				this.cacheStatus = cacheStatus;
		}
}

package com.codingmaple.cache;

import com.codingmaple.cache.constants.CacheEqual;
import com.codingmaple.cache.service.CacheEqualService;

public class GenericCache<T> {
		private final T memoryCache;
		private final T redisCache;

		public GenericCache( T memoryCache, T redisCache) {
				this.memoryCache = memoryCache;
				this.redisCache = redisCache;
		}

		public boolean isSameCache(CacheEqualService<T> cacheEqualService){
				return cacheEqualService.compare( memoryCache, redisCache ) == CacheEqual.SAME;
		}

}

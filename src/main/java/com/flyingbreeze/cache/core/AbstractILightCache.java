package com.flyingbreeze.cache.core;

import com.flyingbreeze.cache.common.Constants;
import com.flyingbreeze.cache.config.StarterServiceProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractILightCache implements ILightCache  {

		protected CacheManager cacheManager;
		protected RedisTemplate<String,Object> redisTemplate;
		protected final StarterServiceProperties properties;
		private static final ThreadLocal<String> CACHE_NAME = new ThreadLocal<>();
		private static final Set<String> CACHE_NAME_CENTRAL = new HashSet<>();

		public AbstractILightCache() {
				this.properties = Constants.Global.applicationContext.getBean( StarterServiceProperties.class );
				init_manager();
		}

		public void registerCacheName(String... cacheNames) {
				for ( String cacheName : cacheNames ) {
						if (CACHE_NAME_CENTRAL.contains(cacheName)) {
								throw new RuntimeException(String.format("%s cacheName has been registered ", cacheName));
						}
						CACHE_NAME_CENTRAL.add(cacheName);
				}
		}


		@SuppressWarnings({"unchecked"})
		private void init_manager() {
				if ( properties.getInfo().getOneLevel().getEnabled()) {
						this.cacheManager = Constants.Global.applicationContext.getBean("breeze-middleware-cache-cacheManager",CacheManager.class);
				}
				if ( properties.getInfo().getTwoLevel().getEnabled() ) {
						this.redisTemplate = Constants.Global.applicationContext.getBean("breeze-middleware-cache-redisTemplate", RedisTemplate.class);
				}
		}


		protected Cache getCache(String cacheName) {
				return cacheManager.getCache( cacheName );
		}

		protected String getCacheKey(String cacheName, String cacheKey ) {
				return Constants.Global.cachedKeyPrefix + "_" + cacheName + cacheKey;
		}


		protected abstract boolean isExistOneCache(String cacheName, String key) ;
		protected abstract boolean isExistTwoCache(String key) ;

		public abstract <T> T loadCache(String cacheName, String key, long timeout, TimeUnit timeUnit, Supplier<T> provider );

		public abstract void clearCache(String cacheName, String key);


		public AbstractILightCache select(String cacheName){
				CACHE_NAME.set(cacheName);
				return this;
		}


		public static void clearCacheName() {
				CACHE_NAME.remove();
		}

		public <T> T loadCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> provider) {
				try {
						return loadCache(getCacheName(), key, timeout, timeUnit, provider);
				}finally {
						clearCacheName();
				}
		}

		public <T> T loadFromOneLevelCache(String key, Supplier<T> provider) {
				try {
						return loadFromOneLevelCache(getCacheName(), key, provider);
				}finally {
						clearCacheName();
				}
		}

		public <T> T loadFromTwoLevelCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> provider) {
				try {
						return loadFromTwoLevelCache(getCacheName(), key, timeout, timeUnit, provider);
				}finally {
						clearCacheName();
				}
		}

		public void clearOneLevelCache(String key) {
				try{
						clearOneLevelCache(getCacheName(), key);
				}finally {
						clearCacheName();
				}
		}


		public void clearTwoLevelCache(String key) {
				try {
						clearTwoLevelCache(getCacheName(), key);
				}finally {
						clearCacheName();
				}
		}

		private String getCacheName() {
				String cacheName = CACHE_NAME.get();
				if ( cacheName == null ) {
						throw new RuntimeException("cacheName can't be null, please check select cacheName ");
				}
				return cacheName;
		}
}

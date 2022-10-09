package com.flyingbreeze.cache.core.impl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.flyingbreeze.cache.core.AbstractILightCache;
import org.springframework.cache.Cache;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LightCache extends AbstractILightCache {

		private final boolean cacheProduct;

		public LightCache() {
				this(true);
		}

		public LightCache( boolean cacheProduct) {
				super();
				this.cacheProduct = cacheProduct;
		}

		@Override
		protected boolean isExistOneCache(String cacheName, String key) {
				return getCache(cacheName).get( key ) != null;
		}

		@Override
		protected boolean isExistTwoCache(String key) {
				return hasKey( key );
		}

		@Override
		public <T> T loadCache(String cacheName, String key, long timeout, TimeUnit timeUnit, Supplier<T> provider) {
			 String cacheKey = getCacheKey( cacheName, key );
			 if ( isExistOneCache( cacheName, key ) ) {
						 if ( cacheProduct ) {
										return deepCopyObj(loadCacheFromLocal(cacheName,key));
						 } else {
										return loadCacheFromLocal(cacheName,key);
						 }
			 } else if ( isExistTwoCache(cacheKey )) {
						 final T data = loadCacheFromRedis(cacheKey);
						 storeCacheToLocal(cacheName, key, data );
						 return data;
			 } else {
						 final T data = provider.get();
						 storeCacheToRedis( cacheKey, data, timeout, timeUnit );
						 storeCacheToLocal( cacheName, key, data );
						 return data;
			 }

		}


		@Override
		public void clearCache(String cacheName, String key) {
				clearOneLevelCache( cacheName, key );
				clearTwoLevelCache( cacheName, key );
		}

		protected boolean hasKey(String key) {
				final Long expire = redisTemplate.getExpire( key );
				return expire != null && expire >= -1;
		}

		@SuppressWarnings("unchecked")
		protected <T> T loadCacheFromRedis(String key) {
				return (T) this.redisTemplate.opsForValue().get( key );
		}

		@SuppressWarnings("unchecked")
		protected <T> T loadCacheFromLocal(String cacheName, String key) {
				final Optional<Cache.ValueWrapper> optional = Optional.ofNullable(getCache(cacheName).get(key));
				return optional.map(valueWrapper -> (T) valueWrapper.get()).orElse(null);
		}

		protected <T> void storeCacheToRedis(String key, T data, long timeout, TimeUnit timeUnit) {
				this.redisTemplate.opsForValue().set( key, data );
				expireKey( key, timeout, timeUnit );
		}

		protected <T> void storeCacheToLocal(String cacheName, String key, T data) {
				getCache(cacheName).put( key, data );
		}

		protected void expireKey(String key, long timeout, TimeUnit timeUnit){
				this.redisTemplate.expire(key, timeout, timeUnit);
		}




		public <T> T loadFromTwoLevelCache(String cacheName, String key, long timeout, TimeUnit timeUnit, Supplier<T> provider) {
				String cacheKey = getCacheKey(cacheName, key );
				if ( isExistTwoCache(cacheKey ) ) {
						return loadCacheFromRedis( cacheKey ) ;
				} else {
						final T data = provider.get();
						storeCacheToRedis( cacheKey, data, timeout, timeUnit );
						return data;
				}
		}

		public <T> T loadFromOneLevelCache(String cacheName, String key, Supplier<T> provider) {
				if ( isExistOneCache( cacheName, key ) ) {
						if (cacheProduct) {
								return deepCopyObj( loadCacheFromLocal(cacheName, key ) ) ;
						} else {
								return loadCacheFromLocal(cacheName, key );
						}
				} else {
						final T data = provider.get();
						storeCacheToLocal( cacheName, key, data );
						return data;
				}
		}

		public void clearOneLevelCache( String cacheName, String key) {
				Optional.ofNullable( getCache(cacheName) ).map( cache -> {
						cache.evict( key );
						return true;
				});
		}

		public void clearTwoLevelCache(String cacheName, String key) {
				this.redisTemplate.delete( getCacheKey(cacheName, key ) ) ;
		}


		@SuppressWarnings("unchecked")
		protected <T> T deepCopyObj(T obj) {
				if ( null == obj ) return null;
				return (T) JSONObject.parseObject(JSON.toJSONString( obj ));
		}

}

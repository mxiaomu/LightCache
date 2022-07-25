package com.codingmaple.cache;

import com.codingmaple.cache.config.properties.GenericCacheProperties;
import com.codingmaple.cache.constants.CacheState;
import com.codingmaple.cache.enums.Mode;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.strategy.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class GenericCacheServiceImpl<T> extends AbstractCacheService<T>{

		private static final Logger log = LoggerFactory.getLogger(GenericCacheServiceImpl.class);

		private final Mode mode;
		private Notification notification;


		public void setNotification(Notification notification) {
				this.notification = notification;
		}

		protected GenericCacheServiceImpl(GenericCacheProperties config,
		                                  CacheRegisterCentral cacheRegisterCentral,
		                                  RedisTemplate<String, Object> redisTemplate,
		                                  CacheManager cacheManager, String cacheName, Class<? super T> clazz,
		                                  StoreType storeType,
		                                  Mode mode,
		                                  SerializationService serializationService,
		                                  Notification notification) {
				super(config, cacheRegisterCentral, redisTemplate, cacheManager, cacheName, clazz, storeType, serializationService);
				this.mode = mode;
				this.notification = notification;
		}


		@Override
		protected Cache getCache() {
				String cacheName = super.getCacheName();
				CacheManager cacheManager = super.getCacheManager();
				return cacheManager.getCache(cacheName);
		}

		@Override
		protected void expireKey(String key, long timeout, TimeUnit timeUnit) {
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				String redisKey = getRedisKey(key);
				if (super.getStoreType() == StoreType.BYTE_ARRAY) {
						byte[] redisKeyBytes = serializeKey(redisKey);
						redisTemplate.execute(connection -> {
								connection.expire(redisKeyBytes, timeUnit.toSeconds(timeout));
								return redisKeyBytes;
						}, true);
				} else {
						redisTemplate.expire(redisKey, timeout, timeUnit);
				}
		}

		@SuppressWarnings("all")
		@Override
		protected boolean hasKey(String key) {
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				String redisKey = getRedisKey(key);
				if (super.getStoreType() == StoreType.BYTE_ARRAY) {
						return redisTemplate.execute(new RedisCallback<Boolean>() {
								@Override
								public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
										final byte[] redisKeyBytes = serializeKey(redisKey);
										final Long ttl = connection.ttl(redisKeyBytes);
										return ttl != null && ttl >= -1;
								}
						}, true);
				} else {
						final Long expire = redisTemplate.getExpire(redisKey);
						return expire != null && expire >= -1;
				}
		}

		@SuppressWarnings("unchecked")
		@Override
		public byte[] serializeKey(String key) {
				final RedisSerializer<String> keySerializer = (RedisSerializer<String>) super.getRedisTemplate().getKeySerializer();
				return keySerializer.serialize(key);
		}

		@Override
		public boolean isExistTwoLevelCache(String key) {
				return hasKey(key);
		}

		@Override
		public boolean isExistOneLevelCache(String key) {
				final Cache cache = getCache();
				return cache != null && cache.get(key) != null;
		}

		@Override
		public CacheState cacheState(String key) {
				final boolean existTwoLevelCache = isExistTwoLevelCache(key);
				final boolean existOneLevelCache = isExistOneLevelCache(key);
				if (!existOneLevelCache && !existTwoLevelCache) {
						return CacheState.NOT_FOUND;
				} else if (!existOneLevelCache) {
						return CacheState.NOT_FOUND_LOCAL;
				} else if (!existTwoLevelCache) {
						return CacheState.NOT_FOUND_REDIS;
				} else {
						return CacheState.OK;
				}
		}

		@Override
		public T reloadCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) {
				removeCache(key);
				return loadDataFromLocalCache(key, timeout, timeUnit, predicate, supplier);
		}

		@SuppressWarnings("all")
		private T getLocalCache(String key) {
				final Cache cache = getCache();
				if (cache == null) return null;
				final Cache.ValueWrapper valueWrapper = cache.get(key);
				if (valueWrapper == null) return null;
				return (T) valueWrapper.get();
		}

		@SuppressWarnings("all")
		private T getRedisCache(String key, boolean isExistTwoLevelCache) {
				String redisKey = getRedisKey(key);
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				final StoreType storeType = getStoreType();
				if (isExistTwoLevelCache) {
						Class<? super T> clazz = getClazz();
						T cachedData;
						switch (storeType) {
								case VALUE:
										cachedData = (T) clazz.cast(redisTemplate.opsForValue().get(redisKey));
										break;
								case LIST:
										cachedData = (T) clazz.cast(redisTemplate.opsForList().range(redisKey, 0, -1));
										break;
								case SET:
										cachedData = (T) clazz.cast(redisTemplate.opsForSet().members(redisKey));
										break;
								case BYTE_ARRAY:
										cachedData = handleByteArrayFromRedis(redisTemplate, redisKey, super.getSerializationService(), clazz);
										break;
								default:
										throw new IllegalStateException("序列化类型错误");
						}
						return cachedData;
				}
				return null;
		}


		@Override
		public T loadDataFromRedisCache(String key, Supplier<T> supplier) {
				long expireTime = super.getCacheConfig().getDefaultExpiryTime();
				return loadDataFromRedisCache(key, expireTime, TimeUnit.SECONDS, supplier);
		}

		@SuppressWarnings("unchecked")
		private T handleByteArrayFromRedis(RedisTemplate<String, Object> redisTemplate, String redisKey, SerializationService serializationService, Class<?> clazz) {

				final byte[] data = redisTemplate.execute((RedisCallback<byte[]>) connection -> connection.get(serializeKey(redisKey)));
				return (T) serializationService.deserialize(data, clazz);

		}


		@Override
		public T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier) {
				return loadDataFromRedisCache(key, timeout, timeUnit, (result) -> true, supplier);
		}

		public T loadDataFromRedisCache(boolean isExistTwoLevelCache, boolean isExistOneLevelCache, String key, long timeout, TimeUnit timeUnit, Predicate<T> cachePredicate, Supplier<T> dataProvider) {
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				final StoreType storeType = getStoreType();
				if (isExistTwoLevelCache) {
						T cachedData = getRedisCache(key, true);
						if (!cachePredicate.test(cachedData)) {
								removeCache(key);
								return dataProvider.get();
						} else {
								log.error("isExistOneLevelCache:{}", isExistOneLevelCache);
								if ( mode == Mode.DISTRIBUTED ) {
										return cachedData;
								}else if ( mode == Mode.MIXTURE ) {
										if (!isExistOneLevelCache) {
												putCache(key, cachedData, true );
										}
								}
								return cachedData;
						}
				} else {
						final T data = dataProvider.get();
						storeCacheToRedis(false, redisTemplate, key, timeout, timeUnit,
										storeType, data);
						return data;
				}
		}

		@Override
		public T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) {
				return loadDataFromRedisCache(isExistTwoLevelCache(key), isExistOneLevelCache(key), key, timeout, timeUnit, predicate, supplier);
		}

		@SuppressWarnings({"unchecked"})
		private void storeCacheToRedis(boolean isExistTwoLevel, RedisTemplate<String, Object> redisTemplate, String key, Long timeout, TimeUnit timeUnit,
		                               StoreType storeType, T data) {
				String redisKey = getRedisKey(key);
				if (isExistTwoLevel) {
						removeRedisCache(key);
				}
				switch (storeType) {
						case VALUE:
								redisTemplate.opsForValue().set(redisKey, data);
								break;
						case LIST:
								List<T> array = (List<T>) data;
								final Object[] objectList = array.toArray(new Object[0]);
								redisTemplate.opsForList().rightPushAll(redisKey, objectList);
								break;
						case SET:
								Set<T> set = (Set<T>) data;
								final Object[] objectSet = set.toArray(new Object[0]);
								redisTemplate.opsForSet().add(redisKey, objectSet);
								break;
						case BYTE_ARRAY:
								storeByteArray(redisTemplate, redisKey, super.getSerializationService(), data);
								break;
						default:
								throw new IllegalStateException("序列化类型错误");
				}
				expireKey(key, timeout, timeUnit);
		}


		private void storeByteArray(RedisTemplate<String, Object> redisTemplate, String redisKey, SerializationService serializationService, T data) {
				final byte[] byteArray = serializationService.serialize(data);
				redisTemplate.execute(connection -> {
						connection.set(serializeKey(redisKey), byteArray);
						return byteArray;
				}, true);
		}

		@Override
		public T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier) {
				return loadDataFromLocalCache(key, timeout, timeUnit, (result) -> true, supplier);
		}

		@Override
		public T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> cachePredicate, Supplier<T> dataProvider) {
				final boolean existOneLevelCache = isExistOneLevelCache(key);
				if (!existOneLevelCache) {
						if ( mode == Mode.SINGLE ) {
								T value = dataProvider.get();
								putCache( key, value , false );
								return value;
						} else if ( mode == Mode.SINGLE_UP ) {
								T value = dataProvider.get();
								putCache( key, value, true );
								return value;
						} else if ( mode == Mode.DISTRIBUTED || mode == Mode.MIXTURE ) {
								return loadDataFromRedisCache(isExistTwoLevelCache(key), false,
												key, timeout, timeUnit, cachePredicate, dataProvider);
						} else {
								return dataProvider.get();
						}
				} else {
						final T localCache = getLocalCache(key);
						if (!cachePredicate.test(localCache)) {
								removeCache(key);
						}
						return convertReadOnlyCache(localCache);
				}
		}



		@Override
		public T loadDataFromLocalCache(String key, Supplier<T> supplier) {
				final Long defaultExpiryTime = super.getCacheConfig().getDefaultExpiryTime();
				return loadDataFromLocalCache(key, defaultExpiryTime, TimeUnit.SECONDS, supplier);
		}

		@Override
		public void removeRedisCache(String key) {
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				String redisKey = getRedisKey(key);
				if (super.getStoreType() == StoreType.BYTE_ARRAY) {
						redisTemplate.execute((RedisCallback<Object>) connection -> connection.del(serializeKey(redisKey)));
				} else {
						redisTemplate.delete(redisKey);
				}
		}

		@Override
		public void removeRedisCache() {
				RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
				if (super.getStoreType() == StoreType.BYTE_ARRAY) {
						redisTemplate.execute(connection -> {
								final Set<byte[]> keys = connection.keys(serializeKey(getRedisKeys()));
								if (keys != null && !keys.isEmpty()) {
										return connection.del(keys.toArray(new byte[0][]));
								}
								return 0L;
						}, true);
				} else {
						final Set<String> keys = scan(getRedisKeys());
						super.getRedisTemplate().delete( keys );
				}
		}

		@Override
		public void removeLocalCache() {
				CacheManager cacheManager = super.getCacheManager();
				String cacheName = super.getCacheName();
				Cache cache = cacheManager.getCache(cacheName);
				if (cache != null) {
						cache.clear();
						if ( isSyncLocal() ) {
								notification.syncNotify( cacheName, null );
						}
				}
		}

		@Override
		public void removeLocalCache(String key) {
				CacheManager cacheManager = super.getCacheManager();
				String cacheName = super.getCacheName();
				Cache cache = cacheManager.getCache(cacheName);
				if (cache != null) {
						cache.evict(key);
						if ( isSyncLocal() ) {
								notification.syncNotify(cacheName, key);
						}
				}
		}

		@Override
		public void removeLocalCache(Cache cache, String key) {
				if (cache != null) {
						cache.evict(key);
						if ( isSyncLocal() ) {
								notification.syncNotify(super.getCacheName(), key);
						}
				}
		}

		@Override
		public void removeLocalCache(Cache cache) {
				if (cache != null) {
						cache.clear();
						if ( isSyncLocal() ) {
								notification.syncNotify(super.getCacheName(), null);
						}
				}
		}

		@Override
		public void removeCache(String key) {
				removeRedisCache(key);
				removeLocalCache(key);
		}

		@Override
		public void removeCache() {
				removeRedisCache();
				removeLocalCache();
		}

		@Override
		public void putCache(Cache cache, String key, Object value, boolean isSync ) {
				removeLocalCache(cache, key);
				final Cache.ValueWrapper valueWrapper = cache.putIfAbsent(key, value);
				if ( isSync ) {
						notification.syncNotify(super.getCacheName(), key);
				}
		}

		@Override
		protected void putCache(String key, Object value) {
				final Cache cache = getCache();
				putCache(cache, key, value, isSyncLocal() );
		}

		@Override
		protected void putCache(String key, Object value, boolean isSync) {
				final Cache cache = getCache();
				putCache(cache, key, value, isSync );
		}

		@SuppressWarnings("all")
		@Override
		public T convertReadOnlyCache(T obj) {
				return (T) super.getSerializationService().deepClone(obj, super.getClazz());
		}

		public String getRedisKey(String key) {
				final String defaultCacheNamePrefix = super.getCacheConfig().getDefaultCacheNamePrefix();
				return defaultCacheNamePrefix + this.getCacheName() + ":" + key;
		}

		public String getRedisKeys() {
				final String defaultCacheNamePrefix = super.getCacheConfig().getDefaultCacheNamePrefix();
				return defaultCacheNamePrefix + this.getCacheName() + ":*";
		}

		private Set<String> getRedisKeySet() {
				final String redisKeys = getRedisKeys();
				return super.getRedisTemplate().keys(redisKeys);
		}

		private boolean isSyncLocal(){
				return mode == Mode.SINGLE_UP || mode == Mode.MIXTURE;
		}


		public Set<String> scan(String pattern) {
				ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
				RedisConnectionFactory factory = super.getRedisTemplate().getConnectionFactory();
				RedisConnection connection = Objects.requireNonNull(factory).getConnection();
				Cursor<byte[]> cursor = connection.scan(options);
				Set<String> result = new HashSet<>();
				while (cursor.hasNext()) {
						result.add(new String(cursor.next()));
				}
				try {
						RedisConnectionUtils.releaseConnection(connection, factory);
				} catch (Exception e) {
						log.error(e.getMessage(), e);
						return null;
				}
				return result;
		}

}

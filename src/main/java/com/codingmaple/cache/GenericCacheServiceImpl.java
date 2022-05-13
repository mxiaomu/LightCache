package com.codingmaple.cache;

import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.constants.CacheState;
import com.codingmaple.cache.constants.SyncType;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.stragety.SyncCacheStrategy;
import com.codingmaple.cache.stragety.impl.TopicSync;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class GenericCacheServiceImpl<T> extends AbstractCacheService<T> implements SyncExecutor {

		private static final Logger log = LoggerFactory.getLogger(GenericCacheServiceImpl.class);

		private static final DefaultRedisScript<Long> DEL_SCRIPT;

		private final ReentrantLock lock = new ReentrantLock();
		private SyncCacheStrategy syncCacheStrategy;

		private final static String DEL_LUA_SCRIPT = "local function scan(key)\n" +
						"    local cursor = 0\n" +
						"    local keynum = 0\n" +
						"    repeat\n" +
						"        local res = redis.call(\"scan\", cursor, \"match\", key)\n" +
						"\n" +
						"        if (res ~= nil and #res >= 0) then\n" +
						"            redis.replicate_commands()\n" +
						"            cursor = tonumber(res[1])\n" +
						"            local ks = res[2]\n" +
						"            keynum = #ks\n" +
						"            for i=1,keynum,1 do\n" +
						"                local k = tostring(ks[i])\n" +
						"                redis.call(\"del\", k)\n" +
						"            end\n" +
						"        end\n" +
						"    until (cursor <= 0)\n" +
						"\n" +
						"    return keynum\n" +
						"end\n" +
						"\n" +
						"local a = #KEYS\n" +
						"local b = 1\n" +
						"local total = 0\n" +
						"while (b <= a)\n" +
						"do\n" +
						"    total = total + scan(KEYS[b])\n" +
						"    b = b + 1\n" +
						"end\n" +
						"\n" +
						"return total";


		static {
				DEL_SCRIPT = getDelScript();
		}


		public static DefaultRedisScript<Long> getDelScript() {
				DefaultRedisScript<Long> script = new DefaultRedisScript<>();
				script.setScriptText(DEL_LUA_SCRIPT);
				script.setResultType(Long.class);
				return script;
		}


		protected GenericCacheServiceImpl(GenericCacheConfig config,
		                                  CacheRegisterCentral cacheRegisterCentral,
		                                  RedissonClient redissonClient,
		                                  RedisTemplate<String, Object> redisTemplate,
		                                  CacheManager cacheManager, String cacheName, Class<? super T> clazz,
		                                  StoreType storeType,
		                                  SerializationService serializationService) {
				super(config, cacheRegisterCentral, redisTemplate, cacheManager, cacheName, clazz, storeType, serializationService);
				if (config.getSyncCache()) {
						syncCacheStrategy = new TopicSync(redissonClient);
						syncCacheStrategy.subscribe(cacheName, this);
				}
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
						log.info("从二级缓存中读取");
						if (!cachePredicate.test(cachedData)) {
								removeCache(key);
								return dataProvider.get();
						} else {
								log.error("isExistOneLevelCache:{}", isExistOneLevelCache);
								if (isSync() && !isExistOneLevelCache) {
										this.syncCacheStrategy.publishSyncCacheEvent(CacheInfo.UpdatedOfCacheInfo(getCacheName(),
														key, timeout, timeUnit));
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
				if (!existOneLevelCache || !isSynced()) {
						return loadDataFromRedisCache(isExistTwoLevelCache(key), false,
										key, timeout, timeUnit, cachePredicate, dataProvider);
				} else {
						final T localCache = getLocalCache(key);
						if (!cachePredicate.test(localCache)) {
								removeCache(key);
						}
						log.info("从一级缓存中读取");
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
				if (isSync()) {
						this.syncCacheStrategy.publishSyncCacheEvent(CacheInfo.RemovedOfCacheInfo(getCacheName(), key));
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
						redisTemplate.execute(DEL_SCRIPT, Collections.singletonList(getRedisKeys()));
				}
				if (isSync()) {
						this.syncCacheStrategy.publishSyncCacheEvent(CacheInfo.RemovedOfCacheInfo(super.getCacheName()));
				}
		}

		@Override
		public void removeLocalCache() {
				CacheManager cacheManager = super.getCacheManager();
				String cacheName = super.getCacheName();
				Cache cache = cacheManager.getCache(cacheName);
				if (cache != null) {
						cache.clear();
				}
		}

		@Override
		public void removeLocalCache(String key) {
				CacheManager cacheManager = super.getCacheManager();
				String cacheName = super.getCacheName();
				Cache cache = cacheManager.getCache(cacheName);
				if (cache != null) {
						cache.evict(key);
				}
		}

		@Override
		public void removeLocalCache(Cache cache, String key) {
				if (cache != null) {
						cache.evict(key);
				}
		}

		@Override
		public void removeLocalCache(Cache cache) {
				if (cache != null) {
						cache.clear();
				}
		}

		@Override
		public void removeCache(String key) {
				removeRedisCache(key);
		}

		@Override
		public void removeCache() {
				removeRedisCache();
		}

		@Override
		public void putCache(Cache cache, String key, Object value) {
				removeLocalCache(cache, key);
				final Cache.ValueWrapper valueWrapper = cache.putIfAbsent(key, value);
		}

		@Override
		public void putCache(String key, Object value) {
				final Cache cache = getCache();
				putCache(cache, key, value);
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

		private boolean isSynced() {
				return !this.lock.isLocked();
		}

		private Set<String> getRedisKeySet() {
				final String redisKeys = getRedisKeys();
				return super.getRedisTemplate().keys(redisKeys);
		}

		private boolean isSync() {
				final List<String> cancelSyncList = super.getCacheConfig().getCancelSyncList();
				boolean existCancel = !cancelSyncList.isEmpty() && cancelSyncList.contains( super.getCacheName() );
				return !existCancel && super.getCacheConfig().getSyncCache();
		}

		@Override
		public void executeSync(CacheInfo cacheInfo) {
				CompletableFuture.runAsync(() -> {
						lock.lock();
						final SyncType syncType = cacheInfo.getSyncType();
						String key = cacheInfo.getKey();
						switch (syncType) {
								case REMOVE_ALL:
										removeLocalCache();
										break;
								case REMOVE_SINGLETON:
										removeLocalCache(key);
										break;
								default:
										T data = getRedisCache(key, true);
										putCache(cacheInfo.getKey(), data);
										break;
						}
				}).whenComplete((resolve, reject) -> {
						try {
								if (reject != null) {
										log.error(reject.getMessage(), reject);
								}
						} finally {
								if (lock.isLocked()) {
										log.info("结束任务");
										lock.unlock();
								}
						}
				});

		}
}

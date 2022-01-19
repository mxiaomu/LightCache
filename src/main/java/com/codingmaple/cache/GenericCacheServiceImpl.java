package com.codingmaple.cache;
import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GenericCacheServiceImpl<T> extends AbstractCacheService<T> {

    private static final DefaultRedisScript<Long> DEL_SCRIPT;

    static {
        DEL_SCRIPT = getDelScript();
    }

    public static DefaultRedisScript<Long> getDelScript() {
        ClassPathResource resource = new ClassPathResource("script/del.lua");
        ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(scriptSource);
        script.setResultType(Long.class);
        return script;
    }


    protected GenericCacheServiceImpl(GenericCacheConfig config, CacheRegisterCentral cacheRegisterCentral, RedisTemplate<String, Object> redisTemplate, CacheManager cacheManager, String cacheName, Class<? super  T> clazz,
                                   StoreType storeType,
                                   SerializationService serializationService) {
        super(config, cacheRegisterCentral, redisTemplate, cacheManager, cacheName, clazz, storeType, serializationService );
    }

    @Override
    public void expireKey(String key, long timeout, TimeUnit timeUnit) {
        RedisTemplate<String,Object> redisTemplate = super.getRedisTemplate();
        String redisKey = getRedisKey( key );
        if ( super.getStoreType() == StoreType.BYTE_ARRAY ){
            byte[] redisKeyBytes = serializeKey( redisKey );
            redisTemplate.execute( connection -> {
                connection.expire( redisKeyBytes , timeUnit.toSeconds( timeout ));
                return redisKeyBytes;
            }, true);
        }else{
            redisTemplate.expire(redisKey, timeout, timeUnit );
        }
    }

    @SuppressWarnings("all")
    @Override
    public boolean hasKey(String key) {
        RedisTemplate<String,Object> redisTemplate = super.getRedisTemplate();
        String redisKey = getRedisKey( key );
        if ( super.getStoreType() == StoreType.BYTE_ARRAY ){
            return redisTemplate.execute(new RedisCallback<Boolean>() {
                @Override
                public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                    final byte[] redisKeyBytes = serializeKey( redisKey );
                    final Long ttl = connection.ttl(redisKeyBytes);
                    return ttl != null && ttl >= -1;
                }
            });
        }else {
            final Long expire = redisTemplate.getExpire(redisKey);
            return expire != null && expire >= -1;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] serializeKey(String key) {
        final RedisSerializer<String> keySerializer = (RedisSerializer<String>) super.getRedisTemplate().getKeySerializer();
        return keySerializer.serialize( key );
    }

    @Override
    public boolean isExistTwoLevelCache(String key) {
        return hasKey( key );
    }


    @Override
    public T loadDataFromRedisCache(String key, Supplier<T> supplier) {
        long expireTime = super.getCacheConfig().getDefaultExpiryTime();
        return loadDataFromRedisCache(key, expireTime , TimeUnit.SECONDS, supplier);
    }

    @SuppressWarnings("unchecked")
    private T handleByteArrayFromRedis(RedisTemplate<String,Object> redisTemplate, String redisKey, SerializationService serializationService, Class<?> clazz){

        final byte[] data = redisTemplate.execute((RedisCallback<byte[]>) connection -> connection.get(serializeKey( redisKey )));
        return (T) serializationService.deserialize( data, clazz);

    }

    @Override
    public T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier) {
       return loadDataFromRedisCache( key, timeout, timeUnit, (result) -> true, supplier );
    }

    @SuppressWarnings("unchecked")
    public T loadDataFromRedisCache(boolean isExistTwoLevelCache, String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) {
        String redisKey = getRedisKey( key );
        RedisTemplate<String, Object> redisTemplate = super.getRedisTemplate();
        final StoreType storeType = getStoreType();
        if ( isExistTwoLevelCache  ) {
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
                    cachedData = (T)clazz.cast(redisTemplate.opsForSet().members(redisKey));
                    break;
                case BYTE_ARRAY:
                    cachedData = handleByteArrayFromRedis( redisTemplate, redisKey, super.getSerializationService(), clazz);
                    break;
                default:
                    throw new IllegalStateException("序列化类型错误");
            }
            if ( !predicate.test( cachedData )){
                final T data = supplier.get();
                storeCacheToRedisAsync( redisTemplate, key, timeout, timeUnit,
                        storeType, data );
                return data;
            }else{
                return cachedData;
            }
        }else{
            final T data = supplier.get();
            storeCacheToRedisAsync( redisTemplate, key, timeout, timeUnit,
                    storeType, data );
            return data;
        }
    }

    @Override
    public T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) {
        return loadDataFromRedisCache( isExistTwoLevelCache(key), key, timeout, timeUnit, predicate, supplier );
    }

    @SuppressWarnings({"unchecked"})
    private void storeCacheToRedisAsync(RedisTemplate<String,Object> redisTemplate, String key, Long timeout, TimeUnit timeUnit,
                                        StoreType storeType, T data ){
        String redisKey = getRedisKey( key );
        CompletableFuture.runAsync( () -> {
            removeRedisCache( key );
            switch ( storeType ){
                case VALUE:
                    redisTemplate.opsForValue().set( redisKey, data );
                    break;
                case LIST:
                    List<T> array = (List<T>) data;
                    final Object[] objectList = array.toArray(new Object[0]);
                    redisTemplate.opsForList().rightPushAll( redisKey, objectList );
                    break;
                case SET:
                    Set<T> set = (Set<T>) data;
                    final Object[] objectSet = set.toArray(new Object[0]);
                    redisTemplate.opsForSet().add( redisKey, objectSet );
                    break;
                case BYTE_ARRAY:
                    storeByteArray( redisTemplate, redisKey, super.getSerializationService(), data );
                    break;
                default:
                    throw new IllegalStateException("序列化类型错误");
            }
            expireKey( key , timeout, timeUnit );
        });
    }

    private void storeByteArray(RedisTemplate<String, Object> redisTemplate, String redisKey, SerializationService serializationService, T data){
        final byte[] byteArray = serializationService.serialize(data);
        redisTemplate.execute(connection -> {
            connection.set( serializeKey( redisKey ),  byteArray);
            return byteArray;
        }, true);
    }

    @Override
    public T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier) {
        return loadDataFromLocalCache( key, timeout, timeUnit, ( result ) -> true, supplier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) {
        String cacheName = super.getCacheName();
        CacheManager cacheManager = super.getCacheManager();
        Cache cache = cacheManager.getCache( cacheName );
        if ( cache != null ){
            T result;
            final Cache.ValueWrapper valueWrapper = cache.get(key);
            // 表示不存在该值的映射
            if ( valueWrapper == null  ){
                result = loadDataFromRedisCache(key, timeout, timeUnit, predicate, supplier);
                putCache(cache, key, result);
            }else{
                boolean isExistTwoLevelCache = isExistTwoLevelCache( key );
                result = (T) valueWrapper.get();
                if (!predicate.test( result ) || !isExistTwoLevelCache) {
                    result = loadDataFromRedisCache(isExistTwoLevelCache, key, timeout, timeUnit, predicate, supplier);
                    putCache(cache, key, result);
                }
            }
            return convertReadOnlyCache(result);
        }
        return loadDataFromRedisCache( key, timeout, timeUnit, predicate, supplier );
    }


    @Override
    public T loadDataFromLocalCache(String key, Supplier<T> supplier) {
        final Long defaultExpiryTime = super.getCacheConfig().getDefaultExpiryTime();
        return loadDataFromLocalCache( key, defaultExpiryTime, TimeUnit.SECONDS, supplier );
    }

    @Override
    public void removeRedisCache( String key ) {
        RedisTemplate<String,Object> redisTemplate = super.getRedisTemplate();
        String redisKey = getRedisKey( key );
        if ( super.getStoreType() == StoreType.BYTE_ARRAY ){
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.del( serializeKey( redisKey ) ));
        }else{
            redisTemplate.delete( redisKey );
        }

    }

    @Override
    public void removeRedisCache() {
        RedisTemplate<String,Object> redisTemplate = super.getRedisTemplate();
        if ( super.getStoreType() == StoreType.BYTE_ARRAY ){
            redisTemplate.execute(connection -> {
                final Set<byte[]> keys = connection.keys(serializeKey(getRedisKeys()));
                if ( keys != null && !keys.isEmpty() ) {
                    return connection.del(keys.toArray(new byte[0][]));
                }
                return 0L;
            }, true);
        }else {
            redisTemplate.execute(DEL_SCRIPT, Collections.singletonList(getRedisKeys()));
        }
    }

    @Override
    public void removeCache( String key ) {
        removeRedisCache( key );
        CacheManager cacheManager = super.getCacheManager();
        String cacheName = super.getCacheName();
        Cache cache = cacheManager.getCache( cacheName );
        if ( cache != null ){
            cache.evictIfPresent( key );
        }
    }

    @Override
    public void removeCache() {
        removeRedisCache();
        CacheManager cacheManager = super.getCacheManager();
        String cacheName = super.getCacheName();
        Cache cache = cacheManager.getCache( cacheName );
        if ( cache != null ){
            cache.invalidate();
        }
    }

    @Override
    public T loadDataFromDataBase(Supplier<T> supplier) {
        return supplier.get();
    }

    @SuppressWarnings("all")
    @Override
    public T convertReadOnlyCache(T obj) {
        return ( T ) super.getSerializationService().deepClone( obj, super.getClazz() );
    }

    public String getRedisKey(String key){
        final String defaultCacheNamePrefix = super.getCacheConfig().getDefaultCacheNamePrefix();
        return  defaultCacheNamePrefix + this.getCacheName() + ":" + key;
    }

    public String getRedisKeys() {
        final String defaultCacheNamePrefix = super.getCacheConfig().getDefaultCacheNamePrefix();
        return defaultCacheNamePrefix + this.getCacheName() + ":*";
    }

    public void putCache(Cache cache, String key, Object value ){
        cache.putIfAbsent( key, value );
    }

}
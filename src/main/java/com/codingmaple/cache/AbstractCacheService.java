package com.codingmaple.cache;
import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractCacheService<T> {


    private CacheManager cacheManager;
    private String cacheName;
    private RedisTemplate<String,Object> redisTemplate;
    private Class<? super T> clazz;
    private SerializationService serializationService;
    private StoreType storeType;
    private final GenericCacheConfig cacheConfig;
    private final CacheRegisterCentral cacheRegisterCentral;



    public AbstractCacheService(GenericCacheConfig genericCacheConfig, CacheRegisterCentral cacheRegisterCentral,
                                RedisTemplate<String, Object> redisTemplate,
                                CacheManager cacheManager,
                                final String cacheName,
                                final Class<? super  T> clazz,
                                StoreType storeType,
                                SerializationService serializationService){
        this.cacheName = cacheName;
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        this.clazz = clazz;
        this.serializationService = serializationService;
        this.storeType = storeType;
        this.cacheConfig = genericCacheConfig;
        this.cacheRegisterCentral = cacheRegisterCentral;

        if ( !cacheRegisterCentral.registerCacheName( cacheName ) ){
            throw new IllegalStateException("存在相同的cacheName: " + cacheName );
        }
    };





    public abstract boolean hasKey(String key);
    public abstract byte[] serializeKey(String key);
    public abstract boolean isExistTwoLevelCache(String key);
    public abstract void expireKey(String key, long timeout, TimeUnit timeUnit);
    public abstract T loadDataFromRedisCache(String key, Supplier<T> supplier);
    public abstract T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier);
    public abstract T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier);
    public abstract T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> supplier) ;
    public abstract T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> supplier) ;
    public abstract T loadDataFromLocalCache(String key, Supplier<T> supplier) ;
    public abstract void removeRedisCache(String key);
    public abstract void removeRedisCache();
    public abstract void removeCache(String key);
    public abstract void removeCache();
    public abstract  T loadDataFromDataBase(Supplier<T> supplier);
    public abstract T convertReadOnlyCache(T obj);


    public void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    public SerializationService getSerializationService() {
        return serializationService;
    }


    public StoreType getStoreType() {
        return storeType;
    }

    public void setStoreType(StoreType storeType) {
        this.storeType = storeType;
    }

    public static StoreType convertServiceType(Class<?> clazz){
        if ( List.class.isAssignableFrom( clazz )){
            return StoreType.LIST;
        }else if (Set.class.isAssignableFrom( clazz )){
            return StoreType.SET;
        }else{
            return StoreType.VALUE;
        }
    }


    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Class<? super T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? super T> clazz) {
        this.clazz = clazz;
    }

    public GenericCacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public CacheRegisterCentral getCacheRegisterService() {
        return cacheRegisterCentral;
    }
}
package com.codingmaple.cache;


import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.constants.CacheState;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.stragety.SyncCacheStrategy;
import org.springframework.cache.Cache;
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

    public AbstractCacheService(GenericCacheConfig genericCacheConfig,
                                CacheRegisterCentral cacheRegisterCentral,
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
            throw new IllegalStateException("存在相同的cacheName, " + cacheName);
        }
    }

    /**
     * 获取 Cache 对象
     * @return
     */
    protected abstract Cache getCache( );

    /**
     * @param key 键 key
     * @return
     */
    protected abstract boolean hasKey(String key);

    /**
     * 序列化 key
     * @param key 键 key
     */
    public abstract byte[] serializeKey(String key);

    /**
     * 是否存在二级缓存
     * @param key 键 key
     */
    public abstract boolean isExistTwoLevelCache(String key);

    /**
     * 是否存在一级缓存
     * @param key 键 key
     */
    public abstract boolean isExistOneLevelCache(String key);

    /**
     * 查询缓存状态
     * @param key 键 key
     * @return
     */
    public abstract CacheState cacheState(String key );

    /**
     * 重新加载缓存
     * @param key 键
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     * @param cachePredicate 验证缓存是否有效
     * @param dataProvider 缓存生产者
     */
    public abstract T reloadCache( String key, long timeout, TimeUnit timeUnit, Predicate<T> cachePredicate, Supplier<T> dataProvider);

    /**
     * 设置过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     */
    protected abstract void expireKey(String key, long timeout, TimeUnit timeUnit);

    /**
     * 从 redis中加载缓存
     * @param key 键 key
     * @param dataProvider 缓存生产者
     */
    public abstract T loadDataFromRedisCache( String key, Supplier<T> dataProvider );

    /**
     * 从 redis 中加载缓存
     * @param key 键 key
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     * @param dataProvider 数据提供方法
     * @return
     */
    public abstract T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> dataProvider);

    /**
     * 从 redis 从 redis 加载缓存
     * @param key 键 key
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     * @param predicate 缓存验证方法
     * @param dataProvider 数据提供方法
     * @return
     */
    public abstract T loadDataFromRedisCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> dataProvider);

    /**
     * 从 本地内存( Cache ) 加载缓存
     * @param key 键 key
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     * @param dataProvider 数据提供方法
     * @return
     */
    public abstract T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Supplier<T> dataProvider) ;

    /**
     * 从 本地内存( Cache ) 加载缓存
     * @param key 键 key
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     * @param predicate 缓存验证数据
     * @param dataProvider 数据提供方法
     * @return
     */
    public abstract T loadDataFromLocalCache(String key, long timeout, TimeUnit timeUnit, Predicate<T> predicate, Supplier<T> dataProvider) ;

    /**
     * 从 本地内存( Cache ) 加载缓存
     * @param key 键 key
     * @param dataProvider 数据提供方法
     * @return
     */
    public abstract T loadDataFromLocalCache(String key, Supplier<T> dataProvider) ;

    /**
     * 删除 redis 缓存 ( 全部 )
     */
    protected abstract void removeRedisCache();

    /**
     * 删除本地缓存(全部)
     */
    protected abstract void removeLocalCache();

    /**
     * 删除指定的键值redis缓存
     * @param key
     */
    protected abstract void removeRedisCache(String key);

    /**
     *  删除指定键的本地缓存
      * @param key
     */
    protected abstract void removeLocalCache(String key);
    protected abstract void removeLocalCache(Cache cache, String key);
    protected abstract void removeLocalCache(Cache cache);
    public abstract void removeCache(String key);
    public abstract void removeCache();

    /**
     * 设置缓存
     * @param cache
     * @param key 键
     * @param value 值
     */
    protected abstract void putCache( Cache cache, String key, Object value );

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     */
    protected abstract void putCache( String key, Object value );

    /**
     * 转化成只读数据
     * @param obj
     * @return
     */
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

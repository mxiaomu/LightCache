package com.codingmaple.cache;
import com.codingmaple.cache.config.properties.GenericCacheProperties;
import com.codingmaple.cache.enums.Mode;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.strategy.Notification;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

public class CacheFactory {
    private  RedisTemplate<String,Object> redisTemplate;
    private final CacheManager cacheManager;
    private final CacheRegisterCentral cacheRegisterCentral;
    private final GenericCacheProperties genericCacheProperties;
    private final SerializationService serializationService;
    private  Notification notification;

    public CacheFactory(CacheManager cacheManager,
                        CacheRegisterCentral registerCentral,
                        GenericCacheProperties genericCacheProperties,
                        SerializationService serializationService){
//        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
        this.cacheRegisterCentral = registerCentral;
        this.genericCacheProperties = genericCacheProperties;
        this.serializationService = serializationService;
//        this.notification = notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> AbstractCacheService<T> createNormalStore(Mode mode, String cacheName, Class<T> clazz){
        return new GenericCacheServiceImpl<>(
                genericCacheProperties,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                AbstractCacheService.convertServiceType(clazz),
                mode,
                serializationService,
                notification
        );
    }

    public <T> AbstractCacheService<T> createByteStore(Mode mode, String cacheName, Class<T> clazz){
        return new GenericCacheServiceImpl<>(
                genericCacheProperties,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                StoreType.BYTE_ARRAY,
                mode,
                serializationService,
                notification
        );
    }

    public <T> AbstractCacheService<T> createByteStore(Mode mode, String cacheName, Class<T> clazz, SerializationService serializationService){
        return new GenericCacheServiceImpl<>(
                genericCacheProperties,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                StoreType.BYTE_ARRAY,
                mode,
                serializationService,
                notification
        );
    }

    private boolean isSyncLocal(Mode mode){
        return mode == Mode.SINGLE_UP || mode == Mode.MIXTURE;
    }



}

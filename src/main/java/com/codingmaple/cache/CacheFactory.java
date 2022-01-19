package com.codingmaple.cache;
import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.serialization.impl.JsonSerializationService;
import com.codingmaple.cache.serialization.impl.KryoSerializationService;
import com.codingmaple.cache.serialization.impl.ProtostuffSerializationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheFactory {

    private final ApplicationContext context;
    private final RedisTemplate<String,Object> redisTemplate;
    private final CacheManager cacheManager;
    private final CacheRegisterCentral cacheRegisterCentral;
    private final GenericCacheConfig genericCacheConfig;
    private final SerializationService serializationService;


    public CacheFactory(ApplicationContext context,
                        RedisTemplate<String, Object> redisTemplate,
                        CacheManager cacheManager, @Qualifier("redisRegisterCentral")CacheRegisterCentral registerCentral,
                        GenericCacheConfig genericCacheConfig){
        this.context = context;
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
        this.cacheRegisterCentral = registerCentral;
        this.genericCacheConfig = genericCacheConfig;
        this.serializationService = selectionService( genericCacheConfig.getSerializationType() );
    }

    private SerializationService selectionService( String serializationType ){
        switch ( serializationType ){
            case "protostuff":
                return this.context.getBean(ProtostuffSerializationService.class);
            case "json":
                return this.context.getBean(JsonSerializationService.class );
            default:
                return this.context.getBean( KryoSerializationService.class );
        }
    }

    public <T> AbstractCacheService<T> createNormalStore(String cacheName, Class<T> clazz){
        return new GenericCacheServiceImpl<T>(
                genericCacheConfig,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                AbstractCacheService.convertServiceType( clazz ),
                serializationService
        );
    }

    public <T> AbstractCacheService<T> createByteStore(String cacheName, Class<T> clazz){
        return new GenericCacheServiceImpl<T>(
                genericCacheConfig,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                StoreType.BYTE_ARRAY,
                serializationService
        );
    }

    public <T> AbstractCacheService<T> createByteStore(String cacheName, Class<T> clazz, SerializationService serializationService){
        return new GenericCacheServiceImpl<T>(
                genericCacheConfig,
                cacheRegisterCentral,
                redisTemplate,
                cacheManager,
                cacheName,
                clazz,
                StoreType.BYTE_ARRAY,
                serializationService
        );
    }


}

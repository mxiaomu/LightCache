package com.codingmaple.cache.config;
import com.codingmaple.cache.CacheFactory;
import com.codingmaple.cache.config.properties.GenericCacheProperties;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.strategy.Notification;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheManagerConfig {

    @Autowired(required = true)
    private GenericCacheProperties genericCacheProperties;

    @Autowired(required = false)
    private Notification notification;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Bean(name = "cacheManager")
    public CacheManager cacheManager( @Qualifier("caffeine") Caffeine<Object,Object> caffeine){
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine( caffeine );
        return manager;
    }

    @Bean
    public Cache<Object, Object> cache(@Qualifier("caffeine") Caffeine<Object, Object> caffeine){
        return caffeine.build();
    }

    @Bean("caffeine")
    public Caffeine<Object,Object> caffeine(){
        return Caffeine.newBuilder().expireAfterWrite(genericCacheProperties.getDefaultExpiryTime(), TimeUnit.SECONDS)
                .maximumSize( genericCacheProperties.getMaximumSize() )
                .initialCapacity( genericCacheProperties.getInitialCapacity() );
    }




    @Bean
    public CacheFactory cacheFactory(GenericCacheProperties genericCacheProperties,
                                     CacheRegisterCentral cacheRegisterCentral,
                                     SerializationService serializationService,
                                     CacheManager cacheManager){
        final CacheFactory cacheFactory = new CacheFactory(cacheManager, cacheRegisterCentral, genericCacheProperties, serializationService);
        cacheFactory.setNotification( notification );
        cacheFactory.setRedisTemplate( redisTemplate );
        return cacheFactory;
    }
}

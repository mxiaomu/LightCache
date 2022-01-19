package com.codingmaple.cache.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheManagerConfig {

    @Autowired
    private GenericCacheConfig genericCacheConfig;

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
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
        return Caffeine.newBuilder().expireAfterWrite(genericCacheConfig.getDefaultExpiryTime(), TimeUnit.SECONDS)
                .maximumSize( genericCacheConfig.getMaximumSize() )
                .initialCapacity( genericCacheConfig.getInitialCapacity() );
    }
}

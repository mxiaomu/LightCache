package com.codingmaple.cache.register;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RegisterHolder {

    @Bean
    public CacheRegisterCentral redisRegisterCentral(RedisTemplate<String,Object> redisTemplate){
        return new RedisCacheRegisterCentralImpl( redisTemplate );
    }

    @Bean
    public CacheRegisterCentral memoryRegisterCentral(){
        return new MemoryCacheRegisterCentralImpl();
    }


}

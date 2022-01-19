package com.codingmaple.cache.register;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class RedisCacheRegisterCentralImpl extends CacheRegisterCentral {

    private static final String REDIS_CACHE_NAME_CENTER_KEY = "cachedCacheManagerCenter";

    private final RedisTemplate<String,Object> redisTemplate;

    public RedisCacheRegisterCentralImpl(RedisTemplate<String,Object> redisTemplate){
        this.redisTemplate = redisTemplate;
        destroyAllCacheNames();
    }

    @Override
    public boolean registerCacheName(String cacheName) {
        final Boolean isExistCacheName = redisTemplate.opsForSet().isMember(REDIS_CACHE_NAME_CENTER_KEY, cacheName);
        if ( Boolean.TRUE.equals( isExistCacheName ) ) {
            return false;
        }
        redisTemplate.opsForSet().add( REDIS_CACHE_NAME_CENTER_KEY, cacheName );
        return true;
    }

    @Override
    public Set<String> getAllCacheNames() {
        return Optional.ofNullable(redisTemplate.opsForSet().members(REDIS_CACHE_NAME_CENTER_KEY)).orElseGet(HashSet::new)
                .stream().map( ( o ) -> (String) o ).collect(Collectors.toSet());
    }

    public void destroyAllCacheNames(){
        this.redisTemplate.delete(REDIS_CACHE_NAME_CENTER_KEY);
    }
}

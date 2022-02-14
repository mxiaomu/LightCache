package com.codingmaple.cache;
import com.codingmaple.cache.config.GenericCacheConfig;
import com.codingmaple.cache.register.CacheRegisterCentral;
import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.serialization.impl.JsonSerializationService;
import com.codingmaple.cache.serialization.impl.KryoSerializationService;
import com.codingmaple.cache.serialization.impl.ProtostuffSerializationService;
import com.codingmaple.cache.stragety.SyncCacheStrategy;
import com.codingmaple.cache.stragety.impl.LastSyncStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class CacheFactory {

    private final ApplicationContext context;
    private final RedisTemplate<String,Object> redisTemplate;
    private final CacheManager cacheManager;
    private final CacheRegisterCentral cacheRegisterCentral;
    private final GenericCacheConfig genericCacheConfig;
    private final SerializationService serializationService;
    private final SyncCacheStrategy defaultSyncCacheStrategy;


    public CacheFactory(ApplicationContext context, RedisProperties redisProperties,
                        CacheManager cacheManager, @Qualifier("redisRegisterCentral") CacheRegisterCentral registerCentral,
                        GenericCacheConfig genericCacheConfig){
        this.context = context;
        this.redisTemplate = registerRedisTemplate( redisProperties );
        RedissonClient redissonClient = registerRedissonClient(redisProperties);
        this.defaultSyncCacheStrategy = new LastSyncStrategy(redissonClient);
        this.cacheManager = cacheManager;
        this.cacheRegisterCentral = registerCentral;
        this.genericCacheConfig = genericCacheConfig;
        this.serializationService = selectionService( genericCacheConfig.getSerializationType() );
    }

    private RedisTemplate<String,Object>  registerRedisTemplate( RedisProperties redisProperties ){
        final GenericObjectPoolConfig<Object> objectGenericObjectPoolConfig = genericObjectPoolConfig(redisProperties);
        final RedisConnectionFactory factory = connectionFactory(objectGenericObjectPoolConfig, redisProperties);
        return getRedisTemplate( factory );
    }

    private RedisConnectionFactory connectionFactory(GenericObjectPoolConfig<Object> genericObjectPoolConfig, RedisProperties redisProperties){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setDatabase(redisProperties.getDatabase());
        configuration.setPassword(redisProperties.getPassword());
        configuration.setHostName(redisProperties.getHost());
        configuration.setPort(redisProperties.getPort());
        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration
                .builder()
                .commandTimeout( redisProperties.getTimeout() )
                .poolConfig( genericObjectPoolConfig )
                .build();
        return new LettuceConnectionFactory(configuration,clientConfiguration);
    }

    private GenericObjectPoolConfig<Object> genericObjectPoolConfig(RedisProperties redisProperties) {
        GenericObjectPoolConfig<Object> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
        genericObjectPoolConfig.setMaxIdle(redisProperties.getLettuce().getPool().getMaxIdle());
        genericObjectPoolConfig.setMinIdle(redisProperties.getLettuce().getPool().getMinIdle());
        genericObjectPoolConfig.setMaxTotal(redisProperties.getLettuce().getPool().getMaxActive());
        genericObjectPoolConfig.setMaxWaitMillis(redisProperties.getLettuce().getPool().getMaxWait().toMillis());
        return genericObjectPoolConfig;
    }

    private RedisTemplate<String, Object> getRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory( redisConnectionFactory );
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer( stringRedisSerializer );
        redisTemplate.setHashKeySerializer( stringRedisSerializer );
        redisTemplate.setValueSerializer( new GenericJackson2JsonRedisSerializer() );
        redisTemplate.setHashValueSerializer( new GenericJackson2JsonRedisSerializer());
        redisTemplate.setDefaultSerializer( jackson2JsonRedisSerializer );
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private RedissonClient registerRedissonClient( RedisProperties redisProperties ){
        Config config = new Config();
        config.useSingleServer()
                .setDatabase( redisProperties.getDatabase() )
                .setAddress( generateRedissonHost( redisProperties.getHost(), redisProperties.getPort() ))
                .setTimeout( (int) redisProperties.getLettuce().getPool().getMaxWait().toMillis() )
                .setConnectionPoolSize( (redisProperties.getLettuce().getPool().getMaxIdle() ))
                .setConnectionMinimumIdleSize( redisProperties.getLettuce().getPool().getMinIdle())
                .setPassword(redisProperties.getPassword().trim());
        return Redisson.create( config );
    }

    private static String generateRedissonHost(String host, int port) {
        return String.format("redis://%s:%d", host, port);
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
                serializationService,
                defaultSyncCacheStrategy
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
                serializationService,
                defaultSyncCacheStrategy
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
                serializationService,
                defaultSyncCacheStrategy
        );
    }




}

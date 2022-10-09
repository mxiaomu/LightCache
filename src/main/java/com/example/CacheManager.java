package com.example;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class CacheManager {

		private final Cache<String, Cache<? super String, CacheWrapper<?>>> CACHE_COLLECTIONS =
						Caffeine.newBuilder()
										.maximumSize(100)
										.build();

		private RedisTemplate<String, Object> redisTemplate;
		private final RedisConnectionFactory connectionFactory;


		public CacheManager(RedisConnectionFactory redisConnectionFactory){
				initRedis(redisConnectionFactory);
				this.connectionFactory = redisConnectionFactory;
		}

		private void initRedis(RedisConnectionFactory connectionFactory){
				this.redisTemplate = new RedisTemplate<>();

				redisTemplate.setConnectionFactory(connectionFactory);

				// 使用Jackson2JsonRedisSerialize 替换默认序列化
				Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
				jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

				// 设置value的序列化规则和 key的序列化规则
				redisTemplate.setKeySerializer(new StringRedisSerializer());
				//jackson2JsonRedisSerializer就是JSON序列化规则，
				redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
				redisTemplate.setHashKeySerializer(new StringRedisSerializer());
				redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
				redisTemplate.afterPropertiesSet();

		}

		public Cache<? super String, CacheWrapper<?>> selectCaffeineCache(String cacheName) {
				return CACHE_COLLECTIONS.get( cacheName, (k) -> Caffeine.newBuilder()
								.expireAfter(new Expiry<String, CacheWrapper<?>>() {
										@Override
										public long expireAfterCreate(@NonNull String key, @NonNull CacheWrapper<?> value, long currentTime) {
												final long timeout = value.getTimeout();
												System.out.println("timeout: " + timeout);;
												if ( timeout <= 0 ) {
														return Long.MAX_VALUE;
												}
												return timeout * 1000_000_000L;
										}

										@Override
										public long expireAfterUpdate(@NonNull String key, @NonNull CacheWrapper<?> value, long currentTime, @NonNegative long currentDuration) {
												return currentDuration;
										}

										@Override
										public long expireAfterRead(@NonNull String key, @NonNull CacheWrapper<?> value, long currentTime, @NonNegative long currentDuration) {
												return currentDuration;
										}
								})
								.scheduler(Scheduler.systemScheduler())
								.build());
		}

		public RedisTemplate<String, Object> selectRedisCache(){
				return this.redisTemplate;
		}

		public long getTimeoutSecs(String cacheName){
				return 0L;
		}

		public <V> CaffeineCache<V> buildCaffeineCache(String cacheName, SyncStrategy strategy) {
				return new CaffeineCache<>(this, strategy, cacheName);
		}

		public <V> RedisCache<V> buildRedisCache(String cacheName){
				return new RedisCache<V>(this, cacheName);
		}

		public SyncStrategy buildRedisSyncStrategy() {
				return new RedisSyncStrategy(connectionFactory, this);
		}

		public <V> GenericCache<V> buildGenericCache(String cacheName, SyncStrategy syncStrategy) {
				final CaffeineCache<V> caffeineCache = buildCaffeineCache(cacheName, syncStrategy);
				final RedisCache<V> redisCache = buildRedisCache(cacheName);
				return new GenericCache<V>(caffeineCache, redisCache);
		}
}

package com.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RedisSyncStrategy implements SyncStrategy{

		private StringRedisTemplate redisTemplate;
		private final CacheManager cacheManager;
		private final String SYNC_TOPIC = "CACHE_SYNC_TOPIC";



		public RedisSyncStrategy(RedisConnectionFactory connectionFactory, CacheManager cacheManager){
				initRedisMessageListener(connectionFactory);
				initRedisTemplate(connectionFactory);
				this.cacheManager = cacheManager;

		}

		public void initRedisMessageListener(RedisConnectionFactory connectionFactory) {
				RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
				redisMessageListenerContainer.setConnectionFactory(connectionFactory);
				redisMessageListenerContainer.addMessageListener((message, pattern) -> {
						String content = new String(message.getBody());
						final String[] contentParts = content.split(",");
						String cacheName = contentParts[0];
						String[] cacheKeys = contentParts[1].split("#");
						handleSyncEvent(cacheName, cacheKeys);

				}, new ChannelTopic(SYNC_TOPIC));
				Jackson2JsonRedisSerializer<String> serializer = new Jackson2JsonRedisSerializer<>(String.class);
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
				serializer.setObjectMapper(objectMapper);
				serializer.setObjectMapper(objectMapper);
				redisMessageListenerContainer.setTopicSerializer(serializer);
		}

		public void initRedisTemplate(RedisConnectionFactory connectionFactory){
				redisTemplate = new StringRedisTemplate();
				redisTemplate.setConnectionFactory(connectionFactory);
				redisTemplate.setKeySerializer(new StringRedisSerializer());
				redisTemplate.setValueSerializer(new StringRedisSerializer());
				redisTemplate.afterPropertiesSet();
		}



		@Override
		public void handleSyncEvent(String cacheName, String... cacheKeys) {
				final Cache<? super String, CacheWrapper<?>> caffeineCache = cacheManager.selectCaffeineCache(cacheName);
				if ( cacheKeys.length == 1 && "*".equals(cacheKeys[0])) {
						caffeineCache.cleanUp();
						return ;
				}
				List<String> cacheKeyList = new ArrayList<>(Arrays.asList(cacheKeys));
				caffeineCache.invalidateAll(cacheKeyList);
		}

		@Override
		public void pushSyncEvent(String cacheName, String... cacheKeys) {
				StringBuilder builder = new StringBuilder(cacheName).append(",");
				for( Object item : cacheKeys ) {
						builder.append(item).append("#");
				}
				String content = builder.toString();
				if ( content.endsWith("#") ) {
						content = content.substring(0, content.length() - 1);
				}
				redisTemplate.convertAndSend(SYNC_TOPIC, content);
		}
}

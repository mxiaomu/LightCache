package com.codingmaple.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@ConditionalOnProperty(prefix = "generic-cache", name = "two-level.enable", havingValue = "true")
@Configuration
public class RedisConfig {

		@Bean
		@ConditionalOnMissingBean
		public RedisConnectionFactory connectionFactory(GenericObjectPoolConfig<Object> genericObjectPoolConfig, RedisProperties redisProperties){
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
				return new LettuceConnectionFactory(configuration, clientConfiguration);
		}

		@Bean
		@ConditionalOnMissingBean
		public GenericObjectPoolConfig<Object> genericObjectPoolConfig(RedisProperties redisProperties) {
				GenericObjectPoolConfig<Object> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
				genericObjectPoolConfig.setMaxIdle(redisProperties.getLettuce().getPool().getMaxIdle());
				genericObjectPoolConfig.setMinIdle(redisProperties.getLettuce().getPool().getMinIdle());
				genericObjectPoolConfig.setMaxTotal(redisProperties.getLettuce().getPool().getMaxActive());
				genericObjectPoolConfig.setMaxWaitMillis(redisProperties.getLettuce().getPool().getMaxWait().toMillis());
				return genericObjectPoolConfig;
		}

		@Bean
		@ConditionalOnMissingBean
		public RedisTemplate<String, Object> redisTemplate (RedisConnectionFactory redisConnectionFactory) {
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
}

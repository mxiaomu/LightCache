package com.flyingbreeze.cache.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import org.springframework.data.redis.serializer.StringRedisSerializer;


@ConditionalOnProperty(prefix = "breeze.middleware.cache.info.two-level", name = "enabled", havingValue = "true")
@Configuration
public class RedisConfig{

		@Autowired
		private RedisProperties redisProperties;

		@Bean(name = "breeze-middleware-cache-redisTemplate")
		public RedisTemplate<String, Object> redisTemplate(@Qualifier("breeze-middleware-cache-redisConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
				RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
				redisTemplate.setConnectionFactory( redisConnectionFactory );
				StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
				FastJson2RedisSerializer serializer = new FastJson2RedisSerializer();
				redisTemplate.setKeySerializer( stringRedisSerializer );
				redisTemplate.setHashKeySerializer( stringRedisSerializer );
				redisTemplate.setValueSerializer( serializer );
				redisTemplate.setHashValueSerializer( serializer );
				redisTemplate.setDefaultSerializer( serializer );
				redisTemplate.afterPropertiesSet();
				return redisTemplate;
		}

		@Bean("breeze-middleware-cache-redisConnectionFactory")
		public RedisConnectionFactory connectionFactory(GenericObjectPoolConfig<Object> genericObjectPoolConfig){
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
		public GenericObjectPoolConfig<Object> genericObjectPoolConfig() {
				GenericObjectPoolConfig<Object> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
				genericObjectPoolConfig.setMaxIdle(redisProperties.getLettuce().getPool().getMaxIdle());
				genericObjectPoolConfig.setMinIdle(redisProperties.getLettuce().getPool().getMinIdle());
				genericObjectPoolConfig.setMaxTotal(redisProperties.getLettuce().getPool().getMaxActive());
				genericObjectPoolConfig.setMaxWait(redisProperties.getLettuce().getPool().getMaxWait());
				return genericObjectPoolConfig;
		}


}

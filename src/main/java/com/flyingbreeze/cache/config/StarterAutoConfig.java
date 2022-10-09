package com.flyingbreeze.cache.config;


import com.flyingbreeze.cache.config.properties.RaftProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "breeze-middleware-cache-starterAutoConfig")
@EnableConfigurationProperties({StarterServiceProperties.class, RaftProperties.class, RedisProperties.class})
public class StarterAutoConfig {

		private StarterServiceProperties serviceProperties;

		public StarterAutoConfig(StarterServiceProperties properties) {
				this.serviceProperties = properties;
		}

		public StarterServiceProperties getServiceProperties() {
				return serviceProperties;
		}

		public void setServiceProperties(StarterServiceProperties serviceProperties) {
				this.serviceProperties = serviceProperties;
		}
}

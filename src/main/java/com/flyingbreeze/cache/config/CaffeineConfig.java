package com.flyingbreeze.cache.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "breeze.middleware.cache.info.one-level", name = "enabled", havingValue = "true")
@Import(StarterAutoConfig.class)
@Configuration
public class CaffeineConfig {


		private final StarterServiceProperties properties;

		public CaffeineConfig(StarterAutoConfig config) {
				this.properties = config.getServiceProperties();
		}


		public Caffeine<Object, Object> createCaffeine() {
				return Caffeine.newBuilder().expireAfterWrite( properties.getDefaultExpire(), TimeUnit.SECONDS )
								.maximumSize( properties.getInfo().getOneLevel().getSettings().getMaximumSize() )
								.initialCapacity( properties.getInfo().getOneLevel().getSettings().getInitialCapacity() );
		}



		@Bean(name = "breeze-middleware-cache-cacheManager")
		public CacheManager cacheManager() {
				final Caffeine<Object, Object> caffeine = createCaffeine();
				CaffeineCacheManager manager = new CaffeineCacheManager();
				manager.setCaffeine( caffeine );
				return manager;
		}


}

package com.flyingbreeze.cache.config;


import com.flyingbreeze.cache.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;



public class LightCacheConfiguration implements ApplicationContextAware, BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {
		private static final Logger logger = LoggerFactory.getLogger(LightCacheConfiguration.class);

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
				Constants.Global.applicationContext = applicationContext;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
				init_config( event.getApplicationContext() );
		}



		public void init_config(ApplicationContext context) {
				try {
						StarterServiceProperties properties = context.getBean("breeze-middleware-cache-starterAutoConfig", StarterAutoConfig.class).getServiceProperties();
						Constants.Global.cachedKeyPrefix = properties.getDefaultCacheNamePrefix();
						Constants.Global.defaultExpireTime = properties.getDefaultExpire();

				} catch (Exception e ) {
						throw new RuntimeException( e );
				}
		}



}

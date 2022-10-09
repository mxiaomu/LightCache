package com.flyingbreeze.cache.annoation;

import com.flyingbreeze.cache.config.CaffeineConfig;
import com.flyingbreeze.cache.config.LightCacheConfiguration;
import com.flyingbreeze.cache.config.RaftConfig;
import com.flyingbreeze.cache.config.RedisConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({LightCacheConfiguration.class})
@ImportAutoConfiguration({RaftConfig.class, RedisConfig.class, CaffeineConfig.class})
@ComponentScan("com.flyingbreeze.*")
public @interface LightCached {
}

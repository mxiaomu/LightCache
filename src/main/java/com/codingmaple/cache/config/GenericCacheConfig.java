package com.codingmaple.cache.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "generic-cache")
public class GenericCacheConfig {

    // 默认过期时间
    private Long defaultExpiryTime = 3600L;
    // 默认缓存前缀
    private String defaultCacheNamePrefix = "cached-";

    private String serializationType = "protostuff";

    private Integer maximumSize = 200;
    private Integer initialCapacity = 100;

    public Long getDefaultExpiryTime() {
        return defaultExpiryTime;
    }

    public void setDefaultExpiryTime(Long defaultExpiryTime) {
        this.defaultExpiryTime = defaultExpiryTime;
    }

    public String getDefaultCacheNamePrefix() {
        return defaultCacheNamePrefix;
    }

    public void setDefaultCacheNamePrefix(String defaultCacheNamePrefix) {
        this.defaultCacheNamePrefix = defaultCacheNamePrefix;
    }

    public String getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }

    public Integer getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(Integer maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Integer getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(Integer initialCapacity) {
        this.initialCapacity = initialCapacity;
    }
}

package com.codingmaple.cache.config.properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "generic-cache")
public class GenericCacheProperties {

    // 默认过期时间
    private Long defaultExpiryTime = 3600L;
    // 默认缓存前缀
    private String defaultCacheNamePrefix = "cached-";

    private String serializationType = "protostuff";

    private Integer maximumSize = 200;
    private Integer initialCapacity = 100;
    private String registerCenter = "memory";

    private OneLevel oneLevel;
    private TwoLevel twoLevel;


    public OneLevel getOneLevel() {
        return oneLevel;
    }

    public void setOneLevel(OneLevel oneLevel) {
        this.oneLevel = oneLevel;
    }

    public TwoLevel getTwoLevel() {
        return twoLevel;
    }

    public void setTwoLevel(TwoLevel twoLevel) {
        this.twoLevel = twoLevel;
    }

    public String getRegisterCenter() {
        return registerCenter;
    }

    public void setRegisterCenter(String registerCenter) {
        this.registerCenter = registerCenter;
    }

    private String mode = "raft";

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

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

    public static class TwoLevel {
        private boolean enable = true;

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public boolean isEnable() {
            return enable;
        }
    }

    public static class OneLevel {
        private Sync sync;

        public Sync getSync() {
            return sync;
        }

        public void setSync(Sync sync) {
            this.sync = sync;
        }
    }

    public static class Sync {
        private boolean enable = false;
        private String mode = "raft";

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}

package com.flyingbreeze.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("breeze.middleware.cache")
public class StarterServiceProperties {

		private Long defaultExpire = 3600L;
		private String defaultCacheNamePrefix = "cached-";
		private Serialization serialization;
		private RegisterCenter registerCenter;
		private Info info;


		public Long getDefaultExpire() {
				return defaultExpire;
		}

		public void setDefaultExpire(Long defaultExpire) {
				this.defaultExpire = defaultExpire;
		}

		public String getDefaultCacheNamePrefix() {
				return defaultCacheNamePrefix;
		}

		public void setDefaultCacheNamePrefix(String defaultCacheNamePrefix) {
				this.defaultCacheNamePrefix = defaultCacheNamePrefix;
		}

		public Serialization getSerialization() {
				return serialization;
		}

		public void setSerialization(Serialization serialization) {
				this.serialization = serialization;
		}

		public RegisterCenter getRegisterCenter() {
				return registerCenter;
		}

		public void setRegisterCenter(RegisterCenter registerCenter) {
				this.registerCenter = registerCenter;
		}

		public Info getInfo() {
				return info;
		}

		public void setInfo(Info info) {
				this.info = info;
		}

		public static class Serialization {
				private String type;

				public String getType() {
						return type;
				}

				public void setType(String type) {
						this.type = type;
				}
		}

		public static class RegisterCenter {
				private String type;

				public String getType() {
						return type;
				}

				public void setType(String type) {
						this.type = type;
				}
		}

		public static class Info {
				private OneLevel oneLevel;
				private TwoLevel twoLevel;

				public OneLevel getOneLevel() {
						return oneLevel;
				}

				public TwoLevel getTwoLevel() {
						return twoLevel;
				}

				public void setOneLevel(OneLevel oneLevel) {
						this.oneLevel = oneLevel;
				}

				public void setTwoLevel(TwoLevel twoLevel) {
						this.twoLevel = twoLevel;
				}
		}

		public static class OneLevel {

				private Boolean enabled = true;
				private Settings settings;
				private Sync sync;

				public Sync getSync() {
						return sync;
				}

				public void setSync(Sync sync) {
						this.sync = sync;
				}

				public Boolean getEnabled() {
						return enabled;
				}

				public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
				}

				public Settings getSettings() {
						return settings;
				}

				public void setSettings(Settings settings) {
						this.settings = settings;
				}
		}

		public static class TwoLevel {
				private Boolean enabled = true;

				public Boolean getEnabled() {
						return enabled;
				}

				public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
				}
		}

		public static class Settings {
				private Integer maximumSize = 200;
				private Integer initialCapacity = 100;

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

		public static class Sync {
				private Boolean enabled = false;
				private String type = "raft";

				public Boolean getEnabled() {
						return enabled;
				}

				public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
				}

				public String getType() {
						return type;
				}

				public void setType(String type) {
						this.type = type;
				}
		}



}

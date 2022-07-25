package com.codingmaple.cache.strategy.impl.raft.rpc;

import java.io.Serializable;

public class SyncRequest implements Serializable {
		private String cacheName;
		private String cacheKey;
		private String source;

		public String getCacheName() {
				return cacheName;
		}

		public void setCacheName(String cacheName) {
				this.cacheName = cacheName;
		}

		public String getCacheKey() {
				return cacheKey;
		}

		public void setCacheKey(String cacheKey) {
				this.cacheKey = cacheKey;
		}

		public String getSource() {
				return source;
		}

		public void setSource(String source) {
				this.source = source;
		}
}

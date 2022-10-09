package com.flyingbreeze.cache.core;

import java.io.Serializable;

public class SyncOperation implements Serializable {
		public byte op;
		private String cacheName;
		private String cacheKey;

		public SyncOperation(final byte op, final String cacheName, final String cacheKey) {
				this.op = op;
				this.cacheName = cacheName;
				this.cacheKey = cacheKey;
		}

		public SyncOperation() {

		}


		public byte getOp() {
				return op;
		}

		public void setOp(byte op) {
				this.op = op;
		}

		public String getCacheName() {
				return cacheName;
		}

		public String getCacheKey() {
				return cacheKey;
		}

		public void setCacheName(String cacheName) {
				this.cacheName = cacheName;
		}

		public void setCacheKey(String cacheKey) {
				this.cacheKey = cacheKey;
		}
}

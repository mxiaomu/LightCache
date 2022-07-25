package com.codingmaple.cache.strategy.impl.raft;

import java.io.Serializable;

public class SyncOperation implements Serializable {
		private static final long serialVersionUID = -6597003954824547294L;
		public static final byte SYNC = 0x01;
		public byte op;
		private final String cacheName;
		private final String cacheKey;

		public static SyncOperation createSync( String cacheName, String cacheKey ) {
				return new SyncOperation( SYNC, cacheName, cacheKey);
		}

		public SyncOperation(byte op, String cacheName, String cacheKey) {
				this.op = op;
				this.cacheName = cacheName;
				this.cacheKey = cacheKey;
		}

		public byte getOp() {
				return op;
		}

		public String getCacheKey() {
				return cacheKey;
		}

		public String getCacheName() {
				return cacheName;
		}
}

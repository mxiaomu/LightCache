package com.flyingbreeze.cache.core;

public class SyncOperationFactory {

		public static final byte SYNC = 0x01;

		public static SyncOperation createSyncOperation(String cacheName, String cacheKey) {
				return new SyncOperation(SYNC, cacheName, cacheKey);
		}
}

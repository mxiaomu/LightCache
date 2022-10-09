package com.example;

/**
 * 同步策略
 */
public interface SyncStrategy {
		void handleSyncEvent(String cacheName, String... cacheKeys);
		void pushSyncEvent(String cacheName, String... cacheKeys);
}

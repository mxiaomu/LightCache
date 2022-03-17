package com.codingmaple.cache;

import com.codingmaple.cache.constants.SyncType;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class CacheInfo implements Serializable {
		private String cacheName;
		private String key;
		private SyncType syncType;
		private Long timeout;
		private TimeUnit timeUnit;

		public CacheInfo(){

		}

		private CacheInfo(String cacheName, String key, SyncType syncType, Long timeout, TimeUnit timeUnit){
				this.cacheName = cacheName;
				this.key = key;
				this.syncType = syncType;
				this.timeout = timeout;
				this.timeUnit = timeUnit;
		}

		private CacheInfo(String cacheName, String key, SyncType syncType ){
				this.cacheName = cacheName;
				this.key = key;
				this.syncType = syncType;
		}


		public static CacheInfo UpdatedOfCacheInfo ( String cacheName, String key, Long timeout, TimeUnit timeUnit) {
				return new CacheInfo( cacheName,key, SyncType.UPDATE, timeout, timeUnit );
		}

		public static CacheInfo RemovedOfCacheInfo ( String cacheName, String key ) {
				return new CacheInfo(cacheName, key, SyncType.REMOVE_SINGLETON );
		}

		public static CacheInfo RemovedOfCacheInfo ( String cacheName ) {
				return new CacheInfo( cacheName, null, SyncType.REMOVE_ALL );
		}

		public String getCacheName() {
				return cacheName;
		}

		public void setCacheName(String cacheName) {
				this.cacheName = cacheName;
		}

		public String getKey() {
				return key;
		}

		public void setKey(String key) {
				this.key = key;
		}

		public Long getTimeout() {
				return timeout;
		}

		public void setTimeout(Long timeout) {
				this.timeout = timeout;
		}

		public TimeUnit getTimeUnit() {
				return timeUnit;
		}

		public void setTimeUnit(TimeUnit timeUnit) {
				this.timeUnit = timeUnit;
		}

		public SyncType getSyncType() {
				return syncType;
		}

		public void setSyncType(SyncType syncType) {
				this.syncType = syncType;
		}
}

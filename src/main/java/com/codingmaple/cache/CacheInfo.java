package com.codingmaple.cache;
import com.codingmaple.cache.constants.SyncType;
import org.checkerframework.checker.units.qual.C;

import java.util.concurrent.TimeUnit;

public class CacheInfo {
		private String key;
		private Object cachedData;
		private SyncType syncType;
		private Long timeout;
		private TimeUnit timeUnit;

		private CacheInfo( String key, Object cachedData, SyncType syncType, Long timeout, TimeUnit timeUnit){
				this.key = key;
				this.cachedData = cachedData;
				this.syncType = syncType;
				this.timeout = timeout;
				this.timeUnit = timeUnit;
		}

		private CacheInfo( String key, SyncType syncType ){
				this.key = key;
				this.syncType = syncType;
		}


		public static CacheInfo UpdatedOfCacheInfo ( String key, Object cachedData, Long timeout, TimeUnit timeUnit) {
				return new CacheInfo( key, cachedData, SyncType.UPDATE, timeout, timeUnit );
		}

		public static CacheInfo RemovedOfCacheInfo ( String key ) {
				return new CacheInfo( key, SyncType.REMOVE_SINGLETON );
		}

		public static CacheInfo RemovedOfCacheInfo ( ) {
				return new CacheInfo( null, SyncType.REMOVE_ALL );
		}





		public String getKey() {
				return key;
		}

		public Object getCachedData() {
				return cachedData;
		}

		public void setCachedData(Object cachedData) {
				this.cachedData = cachedData;
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

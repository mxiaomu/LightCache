package com.codingmaple.cache.strategy.impl.raft;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.error.RaftError;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheRaftSyncServiceImpl implements CacheRaftSyncService{

		private static final Logger log = LoggerFactory.getLogger(CacheRaftSyncServiceImpl.class);


		private final CacheManager cacheManager;
		private static final com.github.benmanes.caffeine.cache.Cache<String, AtomicBoolean> LOCK_CACHE;
		static {
				LOCK_CACHE = Caffeine.newBuilder()
								.expireAfterWrite(2, TimeUnit.HOURS)
								.maximumSize(1000)
								.initialCapacity(100)
								.build();
		}

		public CacheRaftSyncServiceImpl(CacheManager cacheManager) {
				this.cacheManager = cacheManager;
		}

		@Override
		public void syncCache(String cacheName, String cacheKey) {
				final String lockKey = generateLockKey(cacheName, cacheKey);
				try {
						lock(lockKey);
						final Cache cache = cacheManager.getCache(cacheName);
						if ( cache != null ) {
								if (StringUtils.isBlank( cacheKey )) {
										cache.clear();
								} else {
										log.info("删除缓存:{}",cacheKey);
										cache.evict(cacheKey);
								}
						}
				}catch (InterruptedException ignore){

				} finally {
						unlock( lockKey );
				}
		}

		private void lock(String lockKey) throws InterruptedException {
				final AtomicBoolean lock = checkLock(lockKey);
				while (lock != null && lock.compareAndSet( false, true )) {

				}
		}

		private  void unlock(String lockKey) {
				final AtomicBoolean lock = checkLock(lockKey);
				if ( lock == null ) {
						return ;
				}
				lock.set( false );
		}

		private static String generateLockKey(String cacheName, String cacheKey) {
				return "LOCK"+"_"+cacheName + "_" + cacheKey;
		}

		private static AtomicBoolean checkLock(String lockKey) {
				if (LOCK_CACHE.getIfPresent(lockKey) == null) {
						LOCK_CACHE.put(lockKey, new AtomicBoolean( false ));
				}
				return LOCK_CACHE.getIfPresent( lockKey );
		}



}

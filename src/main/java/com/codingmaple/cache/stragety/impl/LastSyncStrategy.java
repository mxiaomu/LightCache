package com.codingmaple.cache.stragety.impl;
import com.codingmaple.cache.CacheInfo;
import com.codingmaple.cache.constants.SyncType;
import com.codingmaple.cache.stragety.SyncCacheStrategy;
import io.lettuce.core.RedisClient;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LastSyncStrategy implements SyncCacheStrategy {

		private final RedissonClient redisClient;
		private final String sync_topic = "SyncTopic";
		private final ConcurrentHashMap<SyncType, Function<Object, Boolean>> concurrentHashMap = new ConcurrentHashMap<>();


		public LastSyncStrategy( RedissonClient redissonClient ) {
				this.redisClient = redissonClient;
				this.redisClient.getTopic(sync_topic)
								.addListener(CacheInfo.class, (channel, cacheInfoMessage) -> {
										final Function<Object,Boolean> function = selectCallback(cacheInfoMessage);
										if ( function == null ){
												throw new RuntimeException("缓存同步 CallBack 获取失败");
										}
										function.apply( cacheInfoMessage.getCachedData() );
								});
		}



		@Override
		public boolean sync(CacheInfo cacheInfo, Function<Object, Boolean> function) {
				concurrentHashMap.putIfAbsent(cacheInfo.getSyncType(), function);
				this.redisClient.getTopic(sync_topic).publish(cacheInfo);
				return true;
		}

		@Override
		public Function<Object, Boolean> selectCallback(CacheInfo cacheInfo) {
				return concurrentHashMap.get( cacheInfo.getSyncType() );
		}


}


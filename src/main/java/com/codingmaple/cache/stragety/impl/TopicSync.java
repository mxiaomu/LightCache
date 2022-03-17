package com.codingmaple.cache.stragety.impl;

import com.codingmaple.cache.CacheInfo;
import com.codingmaple.cache.SyncExecutor;
import com.codingmaple.cache.stragety.SyncCacheStrategy;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TopicSync implements SyncCacheStrategy {

		private static final Logger log = LoggerFactory.getLogger(TopicSync.class);

		private final RedissonClient redissonClient;

		private final String SYNC_TOPIC_PREFIX = "SYNC_TOPIC_";

		public TopicSync(RedissonClient redissonClient){
				this.redissonClient = redissonClient;

		}

		@Override
		public void publishSyncCacheEvent(CacheInfo cacheInfo) {
				log.info("发布消息:{}", cacheInfo.getCacheName());
				String topicName = SYNC_TOPIC_PREFIX + cacheInfo.getCacheName();
				this.redissonClient.getTopic(topicName, new JsonJacksonCodec()).publish( cacheInfo );
				final List<String> channelNames = this.redissonClient.getTopic(SYNC_TOPIC_PREFIX).getChannelNames();
				for (String name : channelNames) {
						log.error( name );
				}
		}



		@Override
		public void subscribe(String cacheName, SyncExecutor syncExecutor) {
				log.error("执行订阅");
				this.redissonClient.getTopic(SYNC_TOPIC_PREFIX + cacheName, new JsonJacksonCodec())
								.addListenerAsync( CacheInfo.class, ( channel, msg ) -> {
										log.info("收到消息:{}， 类型：{}", msg.getCacheName(), msg.getSyncType());
										syncExecutor.executeSync(  msg );
								} );
		}
}

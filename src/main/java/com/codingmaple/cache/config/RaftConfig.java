package com.codingmaple.cache.config;


import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.codingmaple.cache.config.properties.RaftProperties;
import com.codingmaple.cache.strategy.RaftSyncNotification;
import com.codingmaple.cache.strategy.impl.raft.CacheRaftSyncService;
import com.codingmaple.cache.strategy.impl.raft.CacheRaftSyncServiceImpl;
import com.codingmaple.cache.strategy.impl.raft.CacheSyncServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ConditionalOnProperty(prefix = "generic-cache", name = "one-level.sync.enable", havingValue = "true")
@Configuration
public class RaftConfig {

		@Bean
		public CacheSyncServer cacheSyncServer(RaftProperties properties, CacheManager cacheManager) throws IOException {
				final String dataPath = properties.getDataPath();
				final String groupId = properties.getGroupId();
				final PeerId peerId = PeerId.parsePeer(properties.getPeerId());
				final String nodes = properties.getNodes();
				NodeOptions nodeOptions = new NodeOptions();
				com.alipay.sofa.jraft.conf.Configuration initConfig = new com.alipay.sofa.jraft.conf.Configuration();
				if (!initConfig.parse( nodes )){
						throw new IllegalArgumentException("error");
				}
				nodeOptions.setElectionTimeoutMs(1000);
				nodeOptions.setDisableCli(false);
				nodeOptions.setSnapshotIntervalSecs(30);
				nodeOptions.setInitialConf(initConfig);
				return new CacheSyncServer(dataPath, groupId, peerId, nodeOptions, cacheManager );

		}

		@Bean
		public RaftSyncNotification raftSyncNotification(RaftProperties properties) throws InterruptedException, TimeoutException {
				final PeerId peerId = PeerId.parsePeer(properties.getPeerId());
				return new RaftSyncNotification(peerId.getEndpoint().toString(),properties.getGroupId(), properties.getNodes());
		}

		@Bean
		public CacheRaftSyncService cacheRaftSyncService(RaftProperties properties, CacheManager cacheManager, CacheSyncServer cacheSyncServer) {
				return new CacheRaftSyncServiceImpl(cacheManager );
		}
}

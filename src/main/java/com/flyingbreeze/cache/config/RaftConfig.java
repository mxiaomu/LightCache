package com.flyingbreeze.cache.config;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.flyingbreeze.cache.config.properties.RaftProperties;
import com.flyingbreeze.cache.core.CacheRaftSyncService;
import com.flyingbreeze.cache.core.CacheSyncServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "breeze.middleware.cache.info.one-level.sync", name = "enabled", havingValue = "true")
public class RaftConfig {


		@Autowired
		private RaftProperties properties;


		@Bean
		public CacheSyncServer cacheSyncServer(CacheRaftSyncService service) throws IOException {
				final String dataPath = properties.getDataPath();
				final String groupId = properties.getGroupId();
				final PeerId peerId = PeerId.parsePeer(properties.getPeerId());
				final String nodes = properties.getNodes();
				NodeOptions nodeOptions = new NodeOptions();
				com.alipay.sofa.jraft.conf.Configuration initConfig = new com.alipay.sofa.jraft.conf.Configuration();
				if (!initConfig.parse(nodes)){
						throw new IllegalArgumentException("fail to parse nodes");
				}
				nodeOptions.setElectionTimeoutMs(1000);
				nodeOptions.setDisableCli(false);
				nodeOptions.setSnapshotIntervalSecs(30);
				nodeOptions.setInitialConf(initConfig);
				return new CacheSyncServer(dataPath, groupId, peerId, nodeOptions, service );
		}
}

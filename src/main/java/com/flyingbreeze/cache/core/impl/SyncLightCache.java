package com.flyingbreeze.cache.core.impl;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.flyingbreeze.cache.core.CacheRaftSyncService;
import com.flyingbreeze.cache.core.rpc.SyncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

public class SyncLightCache extends LightCache implements CacheRaftSyncService {
		private final Logger logger = LoggerFactory.getLogger(SyncLightCache.class);
		private CliClientServiceImpl cliClientService;
		private final String peerId;
		private final String groupId;
		private final String confStr;

		public SyncLightCache(boolean cacheProduct, String peerId,
		                      String groupId, String confStr) throws InterruptedException, TimeoutException {
				super(cacheProduct);
				this.peerId = peerId;
				this.groupId = groupId;
				this.confStr = confStr;
				cliClientService = null;
		}

		@Override
		public void clearOneLevelCache(String cacheName, String key) {
				super.clearOneLevelCache(cacheName, key);
				syncNotify(cacheName, key);
		}

		@Override
		public void clearCache(String cacheName, String key) {
				super.clearCache(cacheName, key);
				syncNotify(cacheName, key);
		}

		@Override
		public void syncCache(String cacheName, String cacheKey) {
				Optional.ofNullable( getCache(cacheName) ).map(cache -> {
						cache.evict( cacheKey );
						return true;
				});
		}

		public void syncNotify(String cacheName, String cacheKey) {
				if ( cliClientService == null ) {
						synchronized (SyncLightCache.class) {
								try {
										cliClientService = createCliClientService();
								}catch (Exception e) {
										throw new RuntimeException( e );
								}
						}
				}
				final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
				try {
						sync(leader, cacheName, cacheKey );
				}catch (Exception e) {
						logger.error(e.getMessage(), e);
				}
		}

		private CliClientServiceImpl createCliClientService() throws InterruptedException, TimeoutException {
				final Configuration configuration = new Configuration();
				if (!configuration.parse(confStr)) {
						throw new IllegalArgumentException("Fail to parse conf" + confStr);
				}
				RouteTable.getInstance().updateConfiguration(groupId, configuration);
				final CliClientServiceImpl cliClientService = new CliClientServiceImpl();
				cliClientService.init(new CliOptions());
				if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
						throw new IllegalStateException("Refresh leader failed");
				}
				return cliClientService;
		}

		public void sync(PeerId leader, String cacheName, String cacheKey)
				throws RemotingException, InterruptedException{
				SyncRequest request = new SyncRequest();
				request.setCacheName(cacheName);
				request.setCacheKey(cacheKey);
				request.setSource(peerId);

				cliClientService.getRpcClient()
								.invokeAsync(leader.getEndpoint(), request, new InvokeCallback() {
										@Override
										public void complete(Object result, Throwable err) {
												if (err != null) {
														logger.error(err.getMessage(), err);
												}
										}

										@Override
										public Executor executor() {
												return null;
										}
								}, 5000 );
		}
}

package com.codingmaple.cache.strategy;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.codingmaple.cache.strategy.impl.raft.rpc.SyncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

public class RaftSyncNotification implements Notification{
		private static final Logger log = LoggerFactory.getLogger( RaftSyncNotification.class );
		private final String peerId;
		private final String groupId;
		private final String confStr;
		private CliClientServiceImpl cliClientService;

		public RaftSyncNotification(String peerId, String groupId, String confStr) throws InterruptedException, TimeoutException {
				this.peerId = peerId;
				this.groupId = groupId;
				this.confStr = confStr;
				this.cliClientService = null;

		}

		@Override
		public void syncNotify(String cacheName, String cacheKey) {
				if ( cliClientService == null ) {
						try {
								this.cliClientService = createSyncClient();
						}catch (Exception e) {
								log.error(e.getMessage(), e);
						}
				}
				final PeerId leader = RouteTable.getInstance().selectLeader( groupId );
				try {
						sync( leader, cacheName, cacheKey);
				}catch (Exception e){
						e.printStackTrace();
				}

		}

		private CliClientServiceImpl createSyncClient() throws InterruptedException, TimeoutException {
				final Configuration initConfig = new Configuration();
				if (!initConfig.parse( confStr )) {
						throw new IllegalArgumentException("Fail to parse conf:" + confStr);
				}
				RouteTable.getInstance().updateConfiguration(groupId, initConfig);
				final CliClientServiceImpl cliClientService = new CliClientServiceImpl();
				cliClientService.init(new CliOptions());
				if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
						throw new IllegalStateException("Refresh leader failed");
				}
				return cliClientService;
		}

		private void sync( PeerId leader, String cacheName, String cacheKey) throws RemotingException, InterruptedException {
				SyncRequest request = new SyncRequest();
				request.setCacheName( cacheName );
				request.setCacheKey( cacheKey );
				request.setSource( peerId );
				cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, new InvokeCallback() {
						@Override
						public void complete(Object result, Throwable err) {
								if ( err != null ) {
										log.error(err.getMessage(), err);
								}
						}

						@Override
						public Executor executor() {
								return null;
						}
				}, 5000);

		}
}

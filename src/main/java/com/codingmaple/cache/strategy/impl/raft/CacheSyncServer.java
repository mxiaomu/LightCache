package com.codingmaple.cache.strategy.impl.raft;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.codingmaple.cache.strategy.impl.raft.rpc.ResultResponse;
import com.codingmaple.cache.strategy.impl.raft.rpc.SyncRequestProcessor;
import org.apache.commons.io.FileUtils;
import org.springframework.cache.CacheManager;

import java.io.File;
import java.io.IOException;

public class CacheSyncServer {
		private final RaftGroupService raftGroupService;
		private final Node node;
		private final CacheSyncStateMachine fsm;

		public CacheSyncServer(final String dataPath, final String groupId, final PeerId serverId,
		                       final NodeOptions nodeOptions, CacheManager cacheManager) throws IOException{
				FileUtils.forceMkdir(new File(dataPath));
				final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
				CacheRaftSyncService syncService = new CacheRaftSyncServiceImpl(cacheManager);
				rpcServer.registerProcessor(new SyncRequestProcessor(this, syncService));
				this.fsm = new CacheSyncStateMachine("",syncService);
				nodeOptions.setFsm( this.fsm );
				nodeOptions.setLogUri(dataPath + File.separator + "log");
				nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
				nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot");
				this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
				this.node = this.raftGroupService.start();
		}

		public CacheSyncStateMachine getFsm() {
				return this.fsm;
		}

		public Node getNode() {
				return this.node;
		}

		public RaftGroupService RaftGroupService() {
				return this.raftGroupService;
		}

		public ResultResponse redirect() {
				final ResultResponse resultResponse = new ResultResponse();
				resultResponse.setStatus( ResultResponse.SUCCESS );
				if ( this.node != null ) {
						final PeerId leader = this.node.getLeaderId();
						if ( leader != null) {
								resultResponse.setRedirect( leader.toString() );
						}
				}
				return resultResponse;
		}


}

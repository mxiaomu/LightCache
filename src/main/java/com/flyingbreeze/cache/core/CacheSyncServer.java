package com.flyingbreeze.cache.core;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.flyingbreeze.cache.core.rpc.SyncRequestProcessor;
import com.flyingbreeze.cache.core.rpc.SyncResponse;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CacheSyncServer {
		private final RaftGroupService raftGroupService;
		private final Node node;
		private final CacheSyncStateMachine fsm;

		public CacheSyncServer(final String dataPath, final String groupId, final PeerId serverId, final NodeOptions nodeOptions,
		                       CacheRaftSyncService syncService) throws IOException {
				FileUtils.forceMkdir(new File(dataPath));
				final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
				rpcServer.registerProcessor(new SyncRequestProcessor(this));
				String snapshotPath = dataPath + File.separator + "snapshot";
				this.fsm = new CacheSyncStateMachine(syncService);
				nodeOptions.setFsm(this.fsm);
				nodeOptions.setLogUri(dataPath + File.separator + "log");
				nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
				nodeOptions.setSnapshotUri(snapshotPath);
				raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
				this.node = raftGroupService.start();
		}

		public CacheSyncStateMachine getFsm() {
				return fsm;
		}

		public Node getNode() {
				return node;
		}

		public SyncResponse redirect() {
				final SyncResponse response = new SyncResponse();
				response.setStatus(SyncResponse.SUCCESS);
				if ( this.node != null ) {
						final PeerId leader = this.node.getLeaderId();
						if ( leader != null ) {
								response.setRedirect(leader.toString());
						}
				}
				return response;
		}

		public RaftGroupService raftGroupService() {
				return this.raftGroupService;
		}
}

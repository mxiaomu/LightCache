package com.flyingbreeze.cache.core.rpc;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import com.flyingbreeze.cache.core.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class SyncRequestProcessor implements RpcProcessor<SyncRequest> {

		private  final Logger log = LoggerFactory.getLogger(SyncRequestProcessor.class);

		private final CacheSyncServer syncServer;


		public SyncRequestProcessor(CacheSyncServer server) {
				super();
				this.syncServer = server;
		}


		@Override
		public void handleRequest(RpcContext rpcCtx, SyncRequest request) {
				final CacheSyncClosure cacheSyncClosure = new CacheSyncClosure() {
						@Override
						public void run(Status status) {
								rpcCtx.sendResponse(getSyncResponse());
						}
				};

				String cacheName = request.getCacheName();
				String cacheKey = request.getCacheKey();
				applyOperation(SyncOperationFactory.createSyncOperation(cacheName, cacheKey),cacheSyncClosure);
		}

		@Override
		public String interest() {
				return SyncRequest.class.getName();
		}

		private boolean isLeader() {
				return this.syncServer.getFsm().isLeader();
		}

		private String getRedirect() {
				return this.syncServer.redirect().getRedirect();
		}

		private void applyOperation(final SyncOperation op, final CacheSyncClosure closure){
				if (!isLeader()) {
						handleNotLeaderError(closure);
						return ;
				}

				try {
						closure.setSyncOperation( op );
						Task task = new Task();
						task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(SerializerManager.Hessian2)
										.serialize(op)));
						task.setDone(closure);
						this.syncServer.getNode().apply( task );
				}catch (CodecException e) {
						String error = "Fail encode SyncOperation";
						log.error(error, e);
						closure.failure(error, StringUtils.EMPTY);
						closure.run(new Status(RaftError.EINTERNAL, error));
				}
		}

		private void handleNotLeaderError(final CacheSyncClosure closure) {
				closure.failure("Not leader", getRedirect());
				closure.run( new Status(RaftError.EPERM, "Not leader"));
		}
}

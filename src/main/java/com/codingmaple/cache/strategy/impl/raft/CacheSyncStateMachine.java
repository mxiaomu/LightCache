package com.codingmaple.cache.strategy.impl.raft;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Utils;
import com.codingmaple.cache.strategy.impl.raft.snapshot.CacheSyncSnapshotFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CacheSyncStateMachine extends StateMachineAdapter {
		private static final Logger log = LoggerFactory.getLogger(CacheSyncStateMachine.class);
		private final CacheRaftSyncService syncService;
		private final String peerId;

		private final AtomicLong leaderTerm = new AtomicLong( -1 );

		private final AtomicReference<String> value = new AtomicReference<>( " " );

		public boolean isLeader(){
				return this.leaderTerm.get() > 0;
		}
		public String getValue(){
				return this.value.get();
		}


		public CacheSyncStateMachine(String peerId, CacheRaftSyncService syncService) {
				this.syncService = syncService;
				this.peerId = peerId;
		}

		@Override
		public void onApply(Iterator iter) {
				while (iter.hasNext()) {
						SyncOperation syncOperation = null;
						CacheSyncClosure closure = null;
						if ( iter.done() != null ) {
								closure = (CacheSyncClosure) iter.done();
								syncOperation = closure.getSyncOperation();
						} else {
								final ByteBuffer data = iter.getData();
								try {
										syncOperation = SerializerManager.getSerializer(SerializerManager.Hessian2)
														.deserialize( data.array(), SyncOperation.class.getName() );
								}catch (CodecException e) {
										log.error("Fail to decode syncRequest", e);
								}
						}

						if ( syncOperation != null ) {

								switch ( syncOperation.getOp() ) {
										case SyncOperation.SYNC:
												log.info("stateMachine: {}, {}", syncOperation.getCacheName(), syncOperation.getCacheKey());
												final String cacheKey = syncOperation.getCacheKey();
												final String cacheName = syncOperation.getCacheName();
												String content = cacheName + " " + cacheKey;
												value.set( content );
												syncService.syncCache( cacheName, cacheKey );
												break;
								}
								if ( closure != null ) {
										closure.success();
										closure.run(Status.OK());
								}
						}

						iter.next();
				}
		}

		@Override
		public void onSnapshotSave(SnapshotWriter writer, Closure done) {
				final String content = this.value.get();
				Utils.runInThread(() -> {
						final CacheSyncSnapshotFile snapshot = new CacheSyncSnapshotFile(writer.getPath() + File.separator + "data");
						if ( snapshot.save(content) ) {
								if (writer.addFile("data")) {
										done.run(Status.OK());
								} else {
										done.run( new Status(RaftError.EIO, "Fail to add file to writer"));
								}
						} else {
								done.run(new Status(RaftError.EIO, "Fail to save sync snapshot %s", snapshot.getPath()));
						}
				});
		}

		@Override
		public void onError(RaftException e) {
				log.error("Raft error: {}", e, e);
		}

		@Override
		public boolean onSnapshotLoad(SnapshotReader reader) {
				if (isLeader()) {
						log.warn("Leader is not supported to load snapshot");
						return false;
				}
				if ( reader.getFileMeta("data" ) == null ) {
						log.error("Fail to find data file in {}", reader.getPath());
						return false;
				}

				final CacheSyncSnapshotFile snapshot = new CacheSyncSnapshotFile(reader.getPath() + File.separator + "data");
				try {
						this.value.set( snapshot.load() );
						return true;
				}catch (IOException e) {
						log.error("Fail to load snapshot from {}", snapshot.getPath());
						return false;
				}
		}

		@Override
		public void onLeaderStart(long term) {
				this.leaderTerm.set(term);
				super.onLeaderStart(term);
		}

		@Override
		public void onLeaderStop(Status status) {
				this.leaderTerm.set( -1 );
				super.onLeaderStop(status);
		}
}

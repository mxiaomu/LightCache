package com.flyingbreeze.cache.core;

import com.alipay.sofa.jraft.Closure;
import com.flyingbreeze.cache.core.rpc.SyncResponse;

public abstract class CacheSyncClosure implements Closure {

		private SyncResponse response;
		private SyncOperation syncOperation;

		public void setSyncOperation(SyncOperation syncOperation) {
				this.syncOperation = syncOperation;
		}

		public SyncOperation getSyncOperation() {
				return syncOperation;
		}

		public SyncResponse getSyncResponse() {
				return response;
		}

		public void setResultResponse(SyncResponse syncResponse) {
				this.response = syncResponse;
		}

		public void success() {
				setResultResponse(SyncResponse.success( null ));
		}

		public void failure( final String error, final String redirect) {
				setResultResponse(SyncResponse.failure(error, redirect));
		}
}

package com.codingmaple.cache.strategy.impl.raft;

import com.alipay.sofa.jraft.Closure;
import com.codingmaple.cache.strategy.impl.raft.rpc.ResultResponse;

public abstract class CacheSyncClosure implements Closure {
		private ResultResponse resultResponse;
		private SyncOperation syncOperation;

		public void setSyncOperation(SyncOperation syncOperation) {
				this.syncOperation = syncOperation;
		}

		public SyncOperation getSyncOperation() {
				return syncOperation;
		}

		public ResultResponse getResultResponse() {
				return resultResponse;
		}

		public void setResultResponse(ResultResponse resultResponse) {
				this.resultResponse = resultResponse;
		}

		public void success() {
				setResultResponse(ResultResponse.success( null ));
		}

		public void failure( final String error, final String redirect) {
				setResultResponse(ResultResponse.failure(error, redirect));
		}
}

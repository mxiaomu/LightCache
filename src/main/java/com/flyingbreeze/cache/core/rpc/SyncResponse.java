package com.flyingbreeze.cache.core.rpc;

import java.io.Serializable;

public class SyncResponse implements Serializable {
		public static final Integer SUCCESS = 200;
		public static final Integer FAILURE = 500;
		private Integer status;
		private String message;
		private String redirect;

		public SyncResponse(Integer status, String message, String redirect  ) {
				this.status = status;
				this.message = message;
				this.redirect = redirect;
		}

		public SyncResponse(){
				super();
		}
		public static SyncResponse success(String redirect){
				return new SyncResponse(SUCCESS, null, redirect);
		}
		public static SyncResponse failure(String errorMsg, String redirect){
				return new SyncResponse(FAILURE, errorMsg, redirect);
		}

		public Integer getStatus() {
				return status;
		}

		public void setStatus(Integer status) {
				this.status = status;
		}

		public String getMessage() {
				return message;
		}

		public void setMessage(String message) {
				this.message = message;
		}

		public String getRedirect() {
				return redirect;
		}

		public void setRedirect(String redirect) {
				this.redirect = redirect;
		}
}

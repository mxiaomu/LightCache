package com.codingmaple.cache.strategy.impl.raft.rpc;

import java.io.Serializable;


public class ResultResponse implements Serializable {
		public static final Integer SUCCESS = 200;
		public static final Integer FAILURE = 500;
		private Integer status;
		private String message;
		private String redirect;

		public ResultResponse(Integer status, String message, String redirect  ) {
				this.status = status;
				this.message = message;
				this.redirect = redirect;
		}

		public ResultResponse(){
				super();
		}
		public static ResultResponse success(String redirect){
				return new ResultResponse(SUCCESS, null, redirect);
		}
		public static ResultResponse failure(String errorMsg, String redirect){
				return new ResultResponse(FAILURE, errorMsg, redirect);
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

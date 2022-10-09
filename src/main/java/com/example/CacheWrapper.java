package com.example;



import java.io.Serializable;

/**
 * 缓存包装类
 */
public class CacheWrapper<T> implements Serializable {
		private static final long serialVersionUID = 1L;
		private long timeout;
		private T value;

		public CacheWrapper(long timeout, T value) {
				this.timeout = timeout;
				this.value = value;
		}

		public long getTimeout() {
				return timeout;
		}

		public void setTimeout(long timeout) {
				this.timeout = timeout;
		}

		public T getValue() {
				return value;
		}

		public void setValue(T value) {
				this.value = value;
		}
}

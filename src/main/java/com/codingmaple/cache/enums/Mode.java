package com.codingmaple.cache.enums;

/**
 * 缓存持久化模式
 * SINGLE: 单机模式，缓存默认存储在本地进程中，不会同步
 * SINGLE_UP: 单机增强模式，缓存默认存储在本地进程中, 会根据不同的方式进行数据同步更新
 * DISTRIBUTED: 分布式模式，缓存默认存储在redis中, 会根据不同的方式进行数据数据更新
 * MIXTURE: 混合模式，缓存默认存储在一二级缓存中
 */
public enum Mode {
		SINGLE("single"),
		SINGLE_UP("single_up"),
		DISTRIBUTED("distributed"),
		MIXTURE("mixture");
		private final String code;
		Mode(final String code){
				this.code = code;
		}
		public static Mode parseMode(String code){
				for ( Mode mode : values() ) {
						if ( mode.code.equals( code ) ) return mode;
				}
				return MIXTURE;
		}

		public String getCode() {
				return code;
		}
}

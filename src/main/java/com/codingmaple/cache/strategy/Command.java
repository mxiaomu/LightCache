package com.codingmaple.cache.strategy;

public enum Command {
		DEFAULT("0", "默认-删除"),
		EVICT("1", "删除单个值"),
		EVICT_ALL("2", "删除所有值"),
		CREATE("3", "创建");

		private final String code;
		private final String description;
		Command(String code, String description){
			this.code = code;
			this.description = description;
		}

		public String getCode() {
				return code;
		}
		public static Command parseCommand(String code){
				for ( Command command : values() ) {
						if ( command.code.equals( code ) ) {
								return command;
						}
				}
				return Command.EVICT_ALL;
		}

}

package com.flyingbreeze.cache.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "breeze.middleware.cache.raft")
public class RaftProperties {

		private String dataPath;
		private String groupId;
		private String peerId;
		private String nodes;

		public String getDataPath() {
				return dataPath;
		}

		public void setDataPath(String dataPath) {
				this.dataPath = dataPath;
		}

		public String getGroupId() {
				return groupId;
		}

		public void setGroupId(String groupId) {
				this.groupId = groupId;
		}

		public String getPeerId() {
				return peerId;
		}

		public void setPeerId(String peerId) {
				this.peerId = peerId;
		}

		public String getNodes() {
				return nodes;
		}

		public void setNodes(String nodes) {
				this.nodes = nodes;
		}
}

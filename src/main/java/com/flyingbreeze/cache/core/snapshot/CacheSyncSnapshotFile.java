package com.flyingbreeze.cache.core.snapshot;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CacheSyncSnapshotFile {

		private final Logger logger = LoggerFactory.getLogger(CacheSyncSnapshotFile.class);

		private final String path;

		public CacheSyncSnapshotFile(String path) {
				super();
				this.path = path;
		}

		public String getPath() {
				return path;
		}

		public boolean save(String content) {
				try {
						FileUtils.writeStringToFile(new File(path), content, StandardCharsets.UTF_8);
				} catch (Exception e) {
						logger.error(e.getMessage(), e);
						return false;
				}
				return true;
		}

		public String load() throws IOException {
				return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
		}

}

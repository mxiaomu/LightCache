package com.codingmaple.cache.strategy.impl.raft.snapshot;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CacheSyncSnapshotFile {
		private static final Logger log = LoggerFactory.getLogger(CacheSyncSnapshotFile.class);

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
						FileUtils.writeStringToFile(new File(path), content, StandardCharsets.UTF_8 );
						return true;
				} catch (IOException e) {
						log.error(e.getMessage(), e);
						return false;
				}
		}

		public String load() throws IOException {
				return FileUtils.readFileToString(new File( path ), StandardCharsets.UTF_8);
				// throw new IOException("Fail to load snapshot form " + path);
		}
}

package com.codingmaple.cache.stragety;
import com.codingmaple.cache.CacheInfo;

import java.util.function.Function;

public interface SyncCacheStrategy {
		boolean sync(CacheInfo cacheInfo, Function<Object, Boolean> syncCallback);
		Function<Object, Boolean> selectCallback( CacheInfo cacheInfo );
}

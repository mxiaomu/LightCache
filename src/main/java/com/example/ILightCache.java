package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.function.Function;

public interface ILightCache<V> {
		V getIfPresent(@NonNull String key);
		V get(@NonNull String key, long timeoutSecs, @NonNull Function<? super String, ? extends V> mappingFunction);
		void put(@NonNull String key, @NonNull V value);
		void put(@NonNull String key, @NonNull V value, long timeoutSecs);
		void putIfAbsent(@NonNull String key, @NonNull V value, long timeoutSecs);
		void putIfAbsent(@NonNull String key, @NonNull V value);
		void putAll(@NonNull Map<? extends String, ? extends V> map);
		void invalidate(@NonNull String key);
		void invalidateAll(@NonNull Iterable<@NonNull ?> keys);
		void cleanup();
}

package com.codingmaple.cache.register;
import java.util.Set;

public abstract class CacheRegisterCentral {

    public abstract boolean registerCacheName(String cacheName);
    public abstract Set<String> getAllCacheNames();
}

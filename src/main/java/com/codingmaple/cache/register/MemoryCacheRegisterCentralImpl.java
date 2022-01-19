package com.codingmaple.cache.register;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


public class MemoryCacheRegisterCentralImpl extends CacheRegisterCentral {

    private static final Set<String> REGISTER_CENTRAL = new ConcurrentSkipListSet<>();


    public MemoryCacheRegisterCentralImpl(){

    }

    @Override
    public boolean registerCacheName(String cacheName) {
        if ( REGISTER_CENTRAL.contains( cacheName )){
            return false;
        }
        REGISTER_CENTRAL.add( cacheName );
        return true;
    }

    @Override
    public Set<String> getAllCacheNames() {
        return new HashSet<>(REGISTER_CENTRAL);
    }


}

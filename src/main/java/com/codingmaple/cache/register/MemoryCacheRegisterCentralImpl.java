package com.codingmaple.cache.register;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


@ConditionalOnProperty(prefix = "generic-cache", name = "register-center", havingValue = "memory")
@Component
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

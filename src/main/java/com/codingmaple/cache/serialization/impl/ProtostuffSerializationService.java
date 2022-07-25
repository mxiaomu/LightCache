package com.codingmaple.cache.serialization.impl;

import com.codingmaple.cache.serialization.SerializationService;
import com.codingmaple.cache.serialization.impl.util.ProtostuffUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@ConditionalOnProperty(name = "generic-cache.serialization-type",havingValue = "protostuff")
public class ProtostuffSerializationService implements SerializationService {

    @Override
    public <T> byte[] serialize(T obj)  {
       return ProtostuffUtil.serializeToBytes( obj );
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
       return ProtostuffUtil.deserializeFromBytes( data, clazz );
    }

    @Override
    public <T> T deepClone(T obj, Class<T> clazz)  {
        return deserialize( serialize( obj ), clazz );
    }
}

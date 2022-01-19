package com.codingmaple.cache.serialization.impl;

import com.codingmaple.cache.serialization.SerializationService;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service("kryoSerializationService")
public class KryoSerializationService implements SerializationService {

    private static final Kryo kryo;
    static {
        kryo = new Kryo();
        kryo.setRegistrationRequired( false );
        kryo.setInstantiatorStrategy( new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()) );
    }

    @Override
    public <T> byte[] serialize(T obj)  {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Output output = new Output(byteArrayOutputStream);
        ){
            kryo.writeClassAndObject( output, obj );
            output.flush();
            return byteArrayOutputStream.toByteArray();
        }catch ( IOException exception ){
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try(
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( data );
                Input input = new Input( byteArrayInputStream );
        ){
            return (T) kryo.readClassAndObject(input);
        }catch ( IOException exception){
            return null;
        }
    }

    @Override
    public <T> T deepClone(T obj, Class<T> clazz) {
        return kryo.copy( obj );
    }


}

package com.codingmaple.cache.serialization.impl.util;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProtostuffUtil {
    private static final Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();
    private static final Set<Class<?>> WRAPPER_SET = new HashSet<>();

    @SuppressWarnings("all")
    private static final Class<SerializeDeserializeWrapper> WRAPPER_CLASS = SerializeDeserializeWrapper.class;

    @SuppressWarnings("all")
    private static final Schema<SerializeDeserializeWrapper> WRAPPER_SCHEMA = RuntimeSchema.createFrom(WRAPPER_CLASS);

    static {
        WRAPPER_SET.add(List.class);
        WRAPPER_SET.add(ArrayList.class);
        WRAPPER_SET.add(CopyOnWriteArrayList.class);
        WRAPPER_SET.add(LinkedList.class);
        WRAPPER_SET.add(Stack.class);
        WRAPPER_SET.add(Vector.class);

        WRAPPER_SET.add(Map.class);
        WRAPPER_SET.add(HashMap.class);
        WRAPPER_SET.add(TreeMap.class);
        WRAPPER_SET.add(Hashtable.class);
        WRAPPER_SET.add(SortedMap.class);
        WRAPPER_SET.add(Map.class);
        WRAPPER_SET.add(Object.class);
    }


    private static final Objenesis objenesis = new ObjenesisStd( true );

    private ProtostuffUtil(){

    }

    public static <T> void registerWrapperClass(Class<T> clazz) {
        WRAPPER_SET.add(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> Schema<T> getSchema(Class<T> clazz){
        Schema<T> schema = (Schema<T>) cachedSchema.get( clazz );
        if ( schema == null ){
            schema = RuntimeSchema.createFrom( clazz );
            cachedSchema.put( clazz, schema );
        }
        return schema;
    }

    @SuppressWarnings("unchecked")
    public static <T> String serializeToString(T obj){
        Class<T> clazz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE );
        try{
            if ( !WRAPPER_SET.contains( clazz )){
                Schema<T> schema = getSchema( clazz );
                return new String(ProtostuffIOUtil.toByteArray(obj, schema, buffer), StandardCharsets.UTF_8);
            }else {
               SerializeDeserializeWrapper<T> wrapper = SerializeDeserializeWrapper.builder( obj );
               return new String(ProtostuffIOUtil.toByteArray(wrapper, WRAPPER_SCHEMA, buffer), StandardCharsets.UTF_8);
            }
        }catch (Exception e){
            throw new IllegalStateException( e.getMessage(), e );
        }finally {
            buffer.clear();
        }
    }

    public static <T> T deserializeFromString( String data, Class<T> clazz ){
        try{
            if ( !WRAPPER_SET.contains( clazz )) {
                T message = objenesis.newInstance(clazz);
                Schema<T> schema = getSchema(clazz);
                ProtostuffIOUtil.mergeFrom(data.getBytes(StandardCharsets.UTF_8), message, schema);
                return message;
            }else {
               SerializeDeserializeWrapper<T> wrapper = new SerializeDeserializeWrapper<>();
               ProtostuffIOUtil.mergeFrom(data.getBytes(StandardCharsets.UTF_8), wrapper, WRAPPER_SCHEMA);
               return wrapper.getData();
            }
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> byte[] serializeToBytes(T obj){
        Class<T> clazz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE );
        try{
            if (!WRAPPER_SET.contains( clazz )) {
                Schema<T> schema = getSchema(clazz);
                return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
            }else {
                SerializeDeserializeWrapper<T> wrapper = SerializeDeserializeWrapper.builder( obj );
                return ProtostuffIOUtil.toByteArray(wrapper, WRAPPER_SCHEMA, buffer);
            }
        }catch (Exception e){
            throw new IllegalStateException( e.getMessage(), e );
        }finally {
            buffer.clear();
        }
    }

    public static <T> T deserializeFromBytes(byte[] data, Class<T> clazz){
        try{
            if (!WRAPPER_SET.contains( clazz )) {
                T message = (T) objenesis.newInstance(clazz);
                Schema<T> schema = getSchema(clazz);
                ProtostuffIOUtil.mergeFrom(data, message, schema);
                return message;
            } else {
                SerializeDeserializeWrapper<T> wrapper = new SerializeDeserializeWrapper<>();
                ProtostuffIOUtil.mergeFrom(data, wrapper, WRAPPER_SCHEMA);
                return wrapper.getData();
            }
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}

/**
 *
 序列化的时候禁止使用 匿名内部类初始出 集合 或 map !!!!!!
 */
class SerializeDeserializeWrapper<T>{
    private T data;
    public static <T> SerializeDeserializeWrapper<T> builder(T data) {
        SerializeDeserializeWrapper<T> wrapper = new SerializeDeserializeWrapper<>();
        wrapper.setData(data);
        return wrapper;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}


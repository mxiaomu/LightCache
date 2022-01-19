package com.codingmaple.cache.serialization;

public interface SerializationService {
    <T> byte[] serialize(T obj);
    <T> T deserialize(byte[] data, Class<T> clazz);
    <T> T deepClone(T obj, Class<T> clazz);
}

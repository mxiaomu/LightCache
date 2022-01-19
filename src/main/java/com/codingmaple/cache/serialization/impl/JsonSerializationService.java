package com.codingmaple.cache.serialization.impl;
import com.codingmaple.cache.serialization.SerializationService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service("jsonSerializationService")
public class JsonSerializationService implements SerializationService {

    private static final ObjectMapper MAPPER ;
    static {
        MAPPER = generateMapper();
    }

    private static ObjectMapper generateMapper(){
        ObjectMapper customMapper= new ObjectMapper();

        customMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        customMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

        return customMapper;
    }

    @Override
    public <T> byte[] serialize(T obj)  {
        try {
            return MAPPER.writeValueAsBytes(obj);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e );
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz)  {
        try {
            return MAPPER.readValue(data, clazz );
        }catch (Exception e){
            throw new IllegalStateException( e.getMessage(), e );
        }
    }

    @Override
    public <T> T deepClone(T obj, Class<T> clazz) {
        return deserialize( serialize( obj ), clazz );
    }

}

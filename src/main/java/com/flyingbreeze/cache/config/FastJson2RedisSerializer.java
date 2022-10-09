package com.flyingbreeze.cache.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

public class FastJson2RedisSerializer implements RedisSerializer<Object> {

		@Override
		public byte[] serialize(Object object) throws SerializationException {
				if ( null == object ) return new byte[0];
				return JSON.toJSONBytes( object );
		}

		@Override
		public Object deserialize(byte[] bytes) throws SerializationException {
				if ( bytes == null || bytes.length == 0 ) return null;
				return JSONObject.parseObject( new String( bytes, StandardCharsets.UTF_8 ) );
		}
}

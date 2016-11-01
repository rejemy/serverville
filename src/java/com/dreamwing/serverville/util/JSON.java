package com.dreamwing.serverville.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.data.Product;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public final class JSON {
	
	private static final Logger l = LogManager.getLogger(JSON.class);
	
	public static ObjectMapper JsonMapper;
	
	public static MapType StringObjectMapType;
	public static MapType StringIntegerMapType;
	public static MapType StringStringMapType;
	public static MapType StringProductTextMapType;
	
	public static void init()
	{
		JsonMapper = new ObjectMapper();
		JsonMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		JsonMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		if(Boolean.parseBoolean(ServervilleMain.ServerProperties.getProperty("pretty_json")))
		{
			JsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
		}
		JsonMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		
		TypeFactory typeFactory = JsonMapper.getTypeFactory();
		
		StringObjectMapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
		StringIntegerMapType = typeFactory.constructMapType(HashMap.class, String.class, Integer.class);
		StringStringMapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
		StringProductTextMapType = typeFactory.constructMapType(HashMap.class, String.class, Product.ProductText.class);
	}
	
	public static byte[] serializeToBytes(Object data) throws JsonProcessingException
	{
		data = ScriptObjectMirror.wrapAsJSONCompatible(data, null);
		return JsonMapper.writeValueAsBytes(data);
	}
	
	public static ByteBuf serializeToByteBuf(Object data) throws JsonProcessingException
	{
		data = ScriptObjectMirror.wrapAsJSONCompatible(data, null);
		byte jsonData[] = JsonMapper.writeValueAsBytes(data);
		return Unpooled.wrappedBuffer(jsonData);
	}
	
	public static String serializeToString(Object data) throws JsonProcessingException
	{
		data = ScriptObjectMirror.wrapAsJSONCompatible(data, null);
		return JsonMapper.writeValueAsString(data);
	}
	
	public static <T> T deserialize(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException
	{
		return JsonMapper.readValue(json, clazz);
	}
	
	public static <T> T deserialize(Reader json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException
	{
		return JsonMapper.readValue(json, clazz);
	}
	
	public static <T> T deserialize(Object map, Class<T> clazz)
	{
		return JsonMapper.convertValue(map, clazz);
	}
	
	public static <T> T deserialize(InputStream json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException
	{
		return JsonMapper.readValue(json, clazz);
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static <T> T deserialize(Reader json, TypeReference typeRef) throws JsonParseException, JsonMappingException, IOException
	{
		return JsonMapper.<T>readValue(json, typeRef);
	}
	
	public static <T> T deserialize(String json, JavaType jType) throws JsonParseException, JsonMappingException, IOException
	{
		return JsonMapper.<T>readValue(json, jType);
	}
	
	public static Object deserialize(byte[] bytes) throws JsonProcessingException, IOException
	{
		return JsonMapper.readTree(bytes);
	}
	
	public static Object deserialize(String data) throws JsonProcessingException, IOException
	{
		return JsonMapper.readTree(data);
	}
	
	public static String serializeToStringLogError(Object data)
	{
		try {
			data = ScriptObjectMirror.wrapAsJSONCompatible(data, null);
			return JsonMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			l.error("Error serializing json", e);
			return null;
		}
	}
	
	public static <T> T deserializeLogError(String json, Class<T> clazz)
	{
		try {
			return JsonMapper.readValue(json, clazz);
		} catch (IOException e) {
			l.error("Error deserializing json", e);
			return null;
		}
	}
	
	public static <T> T deserializeLogError(String json, JavaType jType)
	{
		try {
			return JsonMapper.readValue(json, jType);
		} catch (IOException e) {
			l.error("Error deserializing json", e);
			return null;
		}
	}
}

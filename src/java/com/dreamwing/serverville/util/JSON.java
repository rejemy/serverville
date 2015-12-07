package com.dreamwing.serverville.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.dreamwing.serverville.ServervilleMain;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JSON {
	
	public static ObjectMapper JsonMapper;
	
	public static void init()
	{
		JsonMapper = new ObjectMapper();
		JsonMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		JsonMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		if(Boolean.parseBoolean(ServervilleMain.ServerProperties.getProperty("pretty_json")))
		{
			JsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
		}
	}
	
	public static byte[] serializeToBytes(Object data) throws JsonProcessingException
	{
		return JsonMapper.writeValueAsBytes(data);
	}
	
	public static ByteBuf serializeToByteBuf(Object data) throws JsonProcessingException
	{
		byte jsonData[] = JsonMapper.writeValueAsBytes(data);
		return Unpooled.wrappedBuffer(jsonData);
	}
	
	public static String serializeToString(Object data) throws JsonProcessingException
	{
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
}

package com.dreamwing.serverville.apimaker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dreamwing.serverville.client.ClientAPI;
import com.dreamwing.serverville.client.ClientHandlerOptions;
import com.dreamwing.serverville.client.ClientMessageInfo;

public class MakeAPIs
{
	public static abstract class ApiDataType
	{

	}
	
	public static class PrimitiveTypeInfo extends ApiDataType
	{
		public PrimitiveType PrimType;
	}
	
	public enum PrimitiveType
	{
		BOOLEAN,
		BYTE,
		CHAR,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		STRING,
		OBJECT
	}
	
	public static class ApiMapTypeInfo extends ApiDataType
	{
		public PrimitiveTypeInfo KeyType;
		public ApiDataType ValueType;
	}
	
	public static class ApiListTypeInfo extends ApiDataType
	{
		public ApiDataType ValueType;
	}
	
	public static class ApiCustomTypeInfo extends ApiDataType
	{
		public String Name;
		public String FullName;
	}
	
	public static class ApiEnumMember
	{
		public String ID;
		public String Value;
	}
	
	public static class ApiEnumInfo extends ApiCustomTypeInfo
	{
		public List<ApiEnumMember> Members;
	}
	
	public static class ApiMessageField
	{
		public String Name;
		public ApiDataType Type;
	}
	
	public static class ApiMessageInfo extends ApiCustomTypeInfo
	{
		public List<ApiMessageField> Fields;
	}
	
	public static class ApiMethodInfo
	{
		public String Name;
		public ApiMessageInfo RequestType;
		public ApiMessageInfo ReplyType;
		public boolean NeedsAuth;
	}
	
	private static List<ApiMethodInfo> apiMethods = new ArrayList<ApiMethodInfo>();
	private static Map<String,ApiCustomTypeInfo> apiMessageLookup = new HashMap<String,ApiCustomTypeInfo>();
	private static Map<String,ApiCustomTypeInfo> apiMessageNames = new HashMap<String,ApiCustomTypeInfo>();
	private static List<ApiCustomTypeInfo> apiCustomTypes = new ArrayList<ApiCustomTypeInfo>();
	
	private static Class<?>[] BoxedTypesArray = {
			Boolean.class,
			Byte.class,
			Character.class,
			Short.class,
			Integer.class,
			Long.class,
			Float.class,
			Double.class,
			String.class,
			Object.class
			};
	
	private static Set<Class<?>> BoxedTypes = new HashSet<Class<?>>();
	

	public static void main(String[] args)
	{
		Method[] methods = ClientAPI.class.getMethods();
		
		for(Class<?> bType : BoxedTypesArray)
		{
			BoxedTypes.add(bType);
		}
		
		
		System.out.println("Generating client APIs");
		
		try
		{
			for(Method method : methods)
			{
				int mods = method.getModifiers();
				if(!Modifier.isStatic(mods) || !Modifier.isPublic(mods))
					continue;
				
				Class<?> params[] = method.getParameterTypes();
				if(params.length != 2)
					continue;
				
				if(!params[1].equals(ClientMessageInfo.class))
					continue;
				
	
				// It's a client API
				addClientMethod(method);
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception validating client api:");
			e.printStackTrace();
			return;
		}

		try
		{
			BrowserClient.writeBrowserClientApi(apiMethods, apiCustomTypes);
		}
		catch(Exception e)
		{
			System.err.println("Exception writing browser client api:");
			e.printStackTrace();
		}
		
		try
		{
			UnityClient.writeUnityClientApi(apiMethods, apiCustomTypes);
		}
		catch(Exception e)
		{
			System.err.println("Exception writing Unity client api:");
			e.printStackTrace();
		}
		
		System.out.println("Done");
	}
	
	private static void addClientMethod(Method method) throws Exception
	{
		ApiMethodInfo info = new ApiMethodInfo();
		
		info.Name = method.getName();
		
		ClientHandlerOptions options = method.getAnnotation(ClientHandlerOptions.class);
		if(options != null)
			info.NeedsAuth = options.auth();
		else
			info.NeedsAuth = true;
		
		Class<?> params[] = method.getParameterTypes();
		
		info.RequestType = getMessageInfo(params[0]);
		info.ReplyType = getMessageInfo(method.getReturnType());
		
		apiMethods.add(info);
	}
	
	private static ApiDataType getDataType(Type fieldType) throws Exception
	{
		if(fieldType instanceof Class<?>)
		{
			Class<?> typeClass = (Class<?>)fieldType;
			
			String fqName = typeClass.getName();
			
			if(fqName.startsWith("com.dreamwing.serverville.client.ClientMessages$"))
			{
				return getMessageInfo(typeClass);
			}
			else if(typeClass.isEnum())
			{
				return getEnumInfo(typeClass);
			}
			else if(typeClass.isPrimitive() || BoxedTypes.contains(typeClass))
			{
				return getPrimitiveType(typeClass);
			}
			
			throw new Exception("Unknown class type in API "+typeClass);
		}
		else if(fieldType instanceof ParameterizedType)
		{
			ParameterizedType pType = (ParameterizedType)fieldType;
			
			if(Map.class.isAssignableFrom((Class<?>)pType.getRawType()))
			{
				return getMapType(pType);
			}
			else if(List.class.isAssignableFrom((Class<?>)(pType.getRawType())))
			{
				return getListType(pType);
			}
			
			throw new Exception("Unknown parameterized type in API "+pType);
		}
		
		throw new Exception("Unknown type in API "+fieldType);
	}
	
	private static ApiMessageInfo getMessageInfo(Class<?> messageClass) throws Exception
	{
		String fqName = messageClass.getName();
		
		if(!fqName.startsWith("com.dreamwing.serverville.client.ClientMessages$"))
		{
			throw new Exception("Not a client message: "+fqName);
		}
		
		ApiMessageInfo info = (ApiMessageInfo)apiMessageLookup.get(fqName);
		if(info != null)
			return info;
		
		info = new ApiMessageInfo();
		info.Name = messageClass.getSimpleName();
		info.FullName = fqName;
		
		if(apiMessageNames.containsKey(info.Name))
		{
			throw new Exception("Duplicate message name: "+info.Name);
		}
		
		info.Fields = new ArrayList<ApiMessageField>();
		
		Field[] fields = messageClass.getFields();
		for(Field field : fields)
		{
			ApiMessageField fieldInfo = new ApiMessageField();
			fieldInfo.Name = field.getName();
			
			fieldInfo.Type = getDataType(field.getGenericType());
			
			info.Fields.add(fieldInfo);
		}
		
		apiMessageLookup.put(fqName, info);
		apiMessageNames.put(info.Name, info);
		apiCustomTypes.add(info);
		
		return info;
	}
	
	private static ApiEnumInfo getEnumInfo(Class<?> enumClass) throws Exception
	{
		if(!enumClass.isEnum())
		{
			throw new Exception("Not an enum: "+enumClass.getName());
		}
		
		String fqName = enumClass.getName();
		
		ApiEnumInfo info = (ApiEnumInfo)apiMessageLookup.get(fqName);
		if(info != null)
			return info;
		
		info = new ApiEnumInfo();
		info.Name = enumClass.getSimpleName();
		info.FullName = fqName;
		
		if(apiMessageNames.containsKey(info.Name))
		{
			throw new Exception("Duplicate enum name: "+info.Name);
		}
		
		info.Members = new ArrayList<ApiEnumMember>();
		
		Method valueAccessor = enumClass.getDeclaredMethod("value");
		
		Object[] enumContstants = enumClass.getEnumConstants();
		for(Object enumConst : enumContstants)
		{
			ApiEnumMember enumMemb = new ApiEnumMember();
			
			enumMemb.ID = enumConst.toString();
			enumMemb.Value = (String)valueAccessor.invoke(enumConst);
			
			//Class<?> constClass = enumConst.getClass();
			//Field name = constClass.getField("name");
			//Field value = constClass.getField("value");
			
			info.Members.add(enumMemb);
		}
		
		apiMessageLookup.put(fqName, info);
		apiMessageNames.put(info.Name, info);
		apiCustomTypes.add(info);
		
		return info;
	}
	
	private static ApiMapTypeInfo getMapType(ParameterizedType mapType) throws Exception
	{
		if(!Map.class.isAssignableFrom((Class<?>)mapType.getRawType()))
		{
			throw new Exception("Not a map: "+mapType.getTypeName());
		}
		
		Type[] genericTypes = mapType.getActualTypeArguments();
		
		ApiMapTypeInfo info = new ApiMapTypeInfo();
		
		info.KeyType = getPrimitiveType((Class<?>)genericTypes[0]);
		info.ValueType = getDataType(genericTypes[1]);
		
		return info;
	}
	
	private static ApiListTypeInfo getListType(ParameterizedType listType) throws Exception
	{
		if(!List.class.isAssignableFrom((Class<?>)listType.getRawType()))
		{
			throw new Exception("Not a list: "+listType.getTypeName());
		}
		
		Type[] genericTypes = listType.getActualTypeArguments();
		
		ApiListTypeInfo info = new ApiListTypeInfo();
		
		info.ValueType = getDataType(genericTypes[0]);
		
		return info;
	}
	
	private static PrimitiveTypeInfo getPrimitiveType(Class<?> primClass) throws Exception
	{
		if(!(primClass.isPrimitive() || BoxedTypes.contains(primClass)))
		{
			throw new Exception("Not a primative: "+primClass.getName());
		}
		
		PrimitiveTypeInfo info = new PrimitiveTypeInfo();
		
		if(primClass.isPrimitive())
		{
			String primType = primClass.getName();
			switch(primType)
			{
			case "boolean":
				info.PrimType = PrimitiveType.BOOLEAN;
				break;
			case "byte":
				info.PrimType = PrimitiveType.BYTE;
				break;
			case "char":
				info.PrimType = PrimitiveType.CHAR;
				break;
			case "short":
				info.PrimType = PrimitiveType.SHORT;
				break;
			case "int":
				info.PrimType = PrimitiveType.INT;
				break;
			case "long":
				info.PrimType = PrimitiveType.LONG;
				break;
			case "float":
				info.PrimType = PrimitiveType.FLOAT;
				break;
			case "double":
				info.PrimType = PrimitiveType.DOUBLE;
				break;
			default:
				throw new Exception("Unknown primative type: "+primType);
			}
			
		}
		else
		{
			if(primClass.equals(Boolean.class))
			{
				info.PrimType = PrimitiveType.BOOLEAN;
			}
			else if(primClass.equals(Byte.class))
			{
				info.PrimType = PrimitiveType.BYTE;
			}
			else if(primClass.equals(Character.class))
			{
				info.PrimType = PrimitiveType.CHAR;
			}
			else if(primClass.equals(Short.class))
			{
				info.PrimType = PrimitiveType.SHORT;
			}
			else if(primClass.equals(Integer.class))
			{
				info.PrimType = PrimitiveType.INT;
			}
			else if(primClass.equals(Long.class))
			{
				info.PrimType = PrimitiveType.LONG;
			}
			else if(primClass.equals(Float.class))
			{
				info.PrimType = PrimitiveType.FLOAT;
			}
			else if(primClass.equals(Double.class))
			{
				info.PrimType = PrimitiveType.DOUBLE;
			}
			else if(primClass.equals(String.class))
			{
				info.PrimType = PrimitiveType.STRING;
			}
			else if(primClass.equals(Object.class))
			{
				info.PrimType = PrimitiveType.OBJECT;
			}
			else
			{
				throw new Exception("Unknown boxed type: "+primClass);
			}
		}
		
		return info;
	}
	
	

}

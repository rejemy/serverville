package com.dreamwing.serverville.apimaker;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.dreamwing.serverville.apimaker.MakeAPIs.ApiCustomTypeInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiDataType;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiEnumInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiEnumMember;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiListTypeInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiMapTypeInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiMessageField;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiMessageInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.ApiMethodInfo;
import com.dreamwing.serverville.apimaker.MakeAPIs.PrimitiveTypeInfo;

public class UnityClient {

	public static void writeUnityClientApi(List<ApiMethodInfo> apiMethods, List<ApiCustomTypeInfo> apiTypes) throws Exception
	{
		System.out.println("Writing Unity client API");
		
		
		Templater messageFile = new Templater("clients/unity/templates/ServervilleMessages.cs.tmpl");
		
		StringBuilder customTypes = new StringBuilder();
		
		for(ApiCustomTypeInfo typeInfo : apiTypes)
		{
			if(typeInfo instanceof ApiEnumInfo)
			{
				ApiEnumInfo enumInfo = (ApiEnumInfo)typeInfo;
				
				customTypes.append("\t[Serializable]\n\tpublic enum ");
				customTypes.append(enumInfo.Name);
				customTypes.append("\n\t{\n");
				
				for(int m=0; m<enumInfo.Members.size(); m++)
				{
					ApiEnumMember member = enumInfo.Members.get(m);
					customTypes.append("\t\t[EnumMember(Value = \"");
					customTypes.append(member.Value);
					customTypes.append("\")]\n");
					customTypes.append(member.ID);
					if(m != enumInfo.Members.size()-1)
						customTypes.append(",\n");
					else
						customTypes.append("\n");
				}
				
				customTypes.append("\t}\n\n");
			}
			else if(typeInfo instanceof ApiMessageInfo)
			{
				ApiMessageInfo messageInfo = (ApiMessageInfo)typeInfo;
				
				customTypes.append("\t[Serializable]\n\tpublic class ");
				customTypes.append(messageInfo.Name);
				customTypes.append("\n\t{\n");
				
				for(ApiMessageField field : messageInfo.Fields)
				{
					customTypes.append("\t\tpublic ");
					customTypes.append(getCsType(field.Type));
					customTypes.append(" ");
					customTypes.append(field.Name);
					customTypes.append(";\n");
				}
				
				customTypes.append("\t}\n\n");
			}
			else
			{
				throw new Exception("Unknown custom type: "+typeInfo);
			}
		}
		
		messageFile.set("Types", customTypes.toString());
		
		messageFile.writeToFile("clients/unity/Assets/ServervilleClient/ServervilleMessages.cs", StandardCharsets.UTF_8);

		
		
		Templater mainFile = new Templater("clients/unity/templates/Serverville.cs.tmpl");
		Templater apiCall = new Templater("clients/unity/templates/api_call.tmpl");
		
		StringBuilder apis = new StringBuilder();
		
		for(ApiMethodInfo method : apiMethods)
		{
			apiCall.set("MethodName", method.Name);
			apiCall.set("ApiName", method.Name);
			
			apiCall.set("ReqType", method.RequestType.Name);
			apiCall.set("ReplyType", method.ReplyType.Name);
			
			apiCall.set("PreCall", "");
			
			if(method.ReplyType.Name.equals("SignInReply"))
			{
				apiCall.set("SuccessClosure", "delegate(SignInReply reply) { SetUserInfo(reply); if(onSuccess != null) { onSuccess(reply); } }");
			}
			else
			{
				apiCall.set("SuccessClosure", "onSuccess");
			}
			
			StringBuilder reqInit = new StringBuilder();
			StringBuilder params = new StringBuilder();
			
			for(int f=0; f<method.RequestType.Fields.size(); f++)
			{
				ApiMessageField param = method.RequestType.Fields.get(f);
				
				params.append(getCsType(param.Type));
				params.append(" ");
				params.append(param.Name);
				params.append(", ");
				
				reqInit.append("\t\t\t\t\t");
				reqInit.append(param.Name);
				reqInit.append(" = ");
				reqInit.append(param.Name);
				if(f != method.RequestType.Fields.size()-1)
					reqInit.append(",\n");
			}
			
			apiCall.set("ReqInit", reqInit.toString());
			apiCall.set("Params", params.toString());
			
			String apiCallStr = apiCall.toString();
			apis.append(apiCallStr);
		}
		
		String apisStr = apis.toString();
		mainFile.set("APIs", apisStr);
		
		mainFile.writeToFile("clients/unity/Assets/ServervilleClient/Serverville.cs", StandardCharsets.UTF_8);
	}
	
	private static String getCsType(ApiDataType type) throws Exception
	{
		if(type instanceof PrimitiveTypeInfo)
		{
			PrimitiveTypeInfo pType = (PrimitiveTypeInfo)type;
			switch(pType.PrimType)
			{
			case BOOLEAN:
				return "bool";
			case BYTE:
				return "byte";
			case SHORT:
				return "short";
			case INT:
				return "int";
			case LONG:
				return "long";
			case FLOAT:
				return "float";
			case DOUBLE:
				return "double";
			case OBJECT:
				return "object";
			case CHAR:
				return "char";
			case STRING:
				return "string";
			default:
				throw new Exception("Unknown API type");
			}
		}
		else if(type instanceof ApiListTypeInfo)
		{
			ApiListTypeInfo lInfo = (ApiListTypeInfo)type;
			
			return "List<"+getCsType(lInfo.ValueType)+">";
		}
		else if(type instanceof ApiMapTypeInfo)
		{
			ApiMapTypeInfo mInfo = (ApiMapTypeInfo)type;
			
			return "Dictionary<"+getCsType(mInfo.KeyType)+","+getCsType(mInfo.ValueType)+">";
		}
		else if(type instanceof ApiEnumInfo)
		{
			ApiEnumInfo cInfo = (ApiEnumInfo)type;
			
			return cInfo.Name;
		}
		else if(type instanceof ApiMessageInfo)
		{
			ApiMessageInfo cInfo = (ApiMessageInfo)type;
			
			return cInfo.Name;
		}
		
		throw new Exception("Unknown API type");
	}
}

package com.dreamwing.serverville.apimaker;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.dreamwing.serverville.util.FileUtil;

public class BrowserClient {

	public static void writeBrowserClientApi(List<ApiMethodInfo> apiMethods, List<ApiCustomTypeInfo> apiTypes) throws Exception
	{
		System.out.println("Writing client browser/JS API");
		
		Templater messageFile = new Templater("clients/browser/templates/serverville_messages.ts.tmpl");
		
		StringBuilder customTypes = new StringBuilder();
		
		for(ApiCustomTypeInfo typeInfo : apiTypes)
		{
			if(typeInfo instanceof ApiEnumInfo)
			{
				ApiEnumInfo enumInfo = (ApiEnumInfo)typeInfo;
				
				customTypes.append("\texport namespace ");
				customTypes.append(enumInfo.Name);
				customTypes.append("\n\t{\n");
				
				for(ApiEnumMember member : enumInfo.Members)
				{
					customTypes.append("\t\texport var ");
					customTypes.append(member.ID);
					customTypes.append(" = \"");
					customTypes.append(member.Value);
					customTypes.append("\";\n");
				}
				
				customTypes.append("\t}\n\n");
			}
			else if(typeInfo instanceof ApiMessageInfo)
			{
				ApiMessageInfo messageInfo = (ApiMessageInfo)typeInfo;
				
				customTypes.append("\texport interface ");
				customTypes.append(messageInfo.Name);
				customTypes.append("\n\t{\n");
				
				for(ApiMessageField field : messageInfo.Fields)
				{
					customTypes.append("\t\t");
					customTypes.append(field.Name);
					customTypes.append(":");
					customTypes.append(getTsType(field.Type));
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
		
		messageFile.writeToFile("clients/browser/src/serverville_messages.ts", StandardCharsets.UTF_8);

		
		
		Templater mainFile = new Templater("clients/browser/templates/serverville.ts.tmpl");
		Templater apiCall = new Templater("clients/browser/templates/api_call.tmpl");
		
		StringBuilder apis = new StringBuilder();
		
		for(ApiMethodInfo method : apiMethods)
		{
			apiCall.set("MethodName", getMethodName(method.Name));
			apiCall.set("ApiName", method.Name);
			
			apiCall.set("ReqType", method.RequestType.Name);
			apiCall.set("ReplyType", method.ReplyType.Name);
			
			if(method.ReplyType.Name.equals("SignInReply"))
			{
				apiCall.set("PreCall", "var self:Serverville = this;");
				apiCall.set("SuccessClosure", "function(reply:SignInReply):void { self.setUserInfo(reply); if(onSuccess) { onSuccess(reply);} }");
			}
			else
			{
				apiCall.set("PreCall", "");
				apiCall.set("SuccessClosure", "onSuccess");
			}
			
			StringBuilder reqInit = new StringBuilder();
			StringBuilder params = new StringBuilder();
			
			for(int f=0; f<method.RequestType.Fields.size(); f++)
			{
				ApiMessageField param = method.RequestType.Fields.get(f);
				
				params.append(param.Name);
				params.append(":");
				params.append(getTsType(param.Type));
				params.append(", ");
				
				reqInit.append("\t\t\t\t\t\"");
				reqInit.append(param.Name);
				reqInit.append("\":");
				reqInit.append(param.Name);
				if(f != method.RequestType.Fields.size()-1)
					reqInit.append(",\n");
			}
			
			apiCall.set("ReqInit", reqInit.toString());
			apiCall.set("Params", params.toString());
			
			apis.append(apiCall.toString());
		}
		
		mainFile.set("APIs", apis.toString());
		
		mainFile.writeToFile("clients/browser/src/serverville.ts", StandardCharsets.UTF_8);
	
		compile();
	}

	private static String getMethodName(String apiName)
	{
		return apiName.substring(0, 1).toLowerCase()+apiName.substring(1);
	}
	
	private static String getTsType(ApiDataType type) throws Exception
	{
		if(type instanceof PrimitiveTypeInfo)
		{
			PrimitiveTypeInfo pType = (PrimitiveTypeInfo)type;
			switch(pType.PrimType)
			{
			case BOOLEAN:
				return "boolean";
			case BYTE:
			case SHORT:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
				return "number";
			case OBJECT:
				return "any";
			case CHAR:
			case STRING:
				return "string";
			default:
				throw new Exception("Unknown API type");
			}
		}
		else if(type instanceof ApiListTypeInfo)
		{
			ApiListTypeInfo lInfo = (ApiListTypeInfo)type;
			
			return "Array<"+getTsType(lInfo.ValueType)+">";
		}
		else if(type instanceof ApiMapTypeInfo)
		{
			ApiMapTypeInfo mInfo = (ApiMapTypeInfo)type;
			
			return "{[key:"+getTsType(mInfo.KeyType)+"]:"+getTsType(mInfo.ValueType)+"}";
		}
		else if(type instanceof ApiEnumInfo)
		{
			return "string";
		}
		else if(type instanceof ApiMessageInfo)
		{
			ApiMessageInfo cInfo = (ApiMessageInfo)type;
			
			return cInfo.Name;
		}
		
		throw new Exception("Unknown API type");
	}
	
	private static void compile() throws Exception
	{
		System.out.println("Compiling client browser/JS API");
		
		String pathString = System.getenv("PATH");
		List<String> pathList = Arrays.asList(pathString.split(":"));
		if(!pathList.contains("/usr/local/bin"))
		{
			pathList = new ArrayList<String>(pathList);
			pathList.add("/usr/local/bin");
			pathString = String.join(":", pathList);
		}
		
		File workingDir = new File(System.getProperty("user.dir"));
		Path clientDir = workingDir.toPath().resolve("clients/browser");
		
		ProcessBuilder pb = new ProcessBuilder(clientDir.resolve("build.sh").toString());
		
		pb.environment().put("PATH", pathString);
		pb.directory(clientDir.toFile());
		
		Process p = pb.start();
		int status = p.waitFor();
		
		if(status != 0)
		{
			String error = FileUtil.readStreamToString(p.getErrorStream(), StandardCharsets.UTF_8);
			throw new Exception("Compile failure: "+error);
		}
	}
	
}

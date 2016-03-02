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
		
		Templater definitionFile = new Templater("clients/browser/templates/serverville.d.ts.tmpl");
		
		Templater messageFile = new Templater("clients/browser/templates/serverville_messages.ts.tmpl");
		
		StringBuilder customTypes = new StringBuilder();
		StringBuilder customTypeDefs = new StringBuilder();
		
		for(ApiCustomTypeInfo typeInfo : apiTypes)
		{
			if(typeInfo instanceof ApiEnumInfo)
			{
				StringBuilder typeInit = new StringBuilder();
				StringBuilder typeDef = new StringBuilder();
				StringBuilder enumDef = new StringBuilder();
				
				ApiEnumInfo enumInfo = (ApiEnumInfo)typeInfo;
				
				String enumType = getEnumMemberName(enumInfo.Name);
				
				typeInit.append("\texport namespace ");
				typeDef.append("\texport namespace ");
				typeInit.append(enumInfo.Name);
				typeDef.append(enumInfo.Name);
				typeInit.append("\n\t{\n");
				typeDef.append("\n\t{\n");
				
				enumDef.append("\texport type ");
				enumDef.append(enumType);
				enumDef.append(" =\n");
				
				for(int m=0; m<enumInfo.Members.size(); m++)
				{
					ApiEnumMember member = enumInfo.Members.get(m);
					
					typeInit.append("\t\texport const ");
					typeInit.append(member.ID);
					typeInit.append(":");
					typeInit.append(enumType);
					typeInit.append(" = \"");
					typeInit.append(member.Value);
					typeInit.append("\";\n");
					
					typeDef.append("\t\texport const ");
					typeDef.append(member.ID);
					typeDef.append(":");
					typeDef.append(enumType);
					typeDef.append(";\n");
					
					enumDef.append("\t\t\"");
					enumDef.append(member.Value);
					if(m == enumInfo.Members.size()-1)
					{
						enumDef.append("\";\n\n");
					}
					else
					{
						enumDef.append("\" |\n");
					}
				}
				
				typeInit.append("\t}\n\n");
				typeDef.append("\t}\n\n");
				
				customTypes.append(typeInit);
				customTypes.append(enumDef);
				
				customTypeDefs.append(typeDef);
				customTypeDefs.append(enumDef);
			}
			else if(typeInfo instanceof ApiMessageInfo)
			{
				StringBuilder typeDef = new StringBuilder();
				
				ApiMessageInfo messageInfo = (ApiMessageInfo)typeInfo;
				
				typeDef.append("\texport interface ");
				typeDef.append(messageInfo.Name);
				typeDef.append("\n\t{\n");
				
				for(ApiMessageField field : messageInfo.Fields)
				{
					typeDef.append("\t\t");
					typeDef.append(field.Name);
					typeDef.append(":");
					typeDef.append(getTsType(field.Type));
					typeDef.append(";\n");
				}
				
				typeDef.append("\t}\n\n");
				
				customTypes.append(typeDef);
				customTypeDefs.append(typeDef);
			}
			else
			{
				throw new Exception("Unknown custom type: "+typeInfo);
			}
			
		}
		
		messageFile.set("Types", customTypes.toString());
		definitionFile.set("Types", customTypeDefs.toString());
		
		
		messageFile.writeToFile("clients/browser/src/serverville_messages.ts", StandardCharsets.UTF_8);

		
		
		Templater mainFile = new Templater("clients/browser/templates/serverville.ts.tmpl");
		Templater apiCall = new Templater("clients/browser/templates/api_call.tmpl");
		Templater apiDef = new Templater("clients/browser/templates/api_def.tmpl");
		
		StringBuilder apis = new StringBuilder();
		StringBuilder apiDefs = new StringBuilder();
		
		
		for(ApiMethodInfo method : apiMethods)
		{
			apiCall.clear();
			String methodName = getMethodName(method.Name);
			apiCall.set("MethodName", methodName);
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
			
			
			apiDef.clear();
			apiDef.set("MethodName", methodName);
			apiDef.set("ReqType", method.RequestType.Name);
			apiDef.set("ReplyType", method.ReplyType.Name);
			apiDef.set("Params", params.toString());
			
			apiDefs.append(apiDef.toString());
		}
		
		mainFile.set("APIs", apis.toString());
		definitionFile.set("APIs", apiDefs.toString());
		
		mainFile.writeToFile("clients/browser/src/serverville.ts", StandardCharsets.UTF_8);
	
		definitionFile.writeToFile("clients/browser/src/serverville.d.ts", StandardCharsets.UTF_8);
		
		compile();
	}

	private static String getMethodName(String apiName)
	{
		return apiName.substring(0, 1).toLowerCase()+apiName.substring(1);
	}
	
	private static String getEnumMemberName(String typeName)
	{
		return typeName+"Enum";
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
			ApiEnumInfo eInfo = (ApiEnumInfo)type;
			
			return getEnumMemberName(eInfo.Name);
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

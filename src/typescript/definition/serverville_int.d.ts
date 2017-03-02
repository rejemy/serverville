
declare class JsonDataTypeItem {}

declare namespace JsonDataType
{
	var NULL:JsonDataTypeItem;
	var BOOLEAN:JsonDataTypeItem;
	var NUMBER:JsonDataTypeItem;
	var STRING:JsonDataTypeItem;
	var JSON:JsonDataTypeItem;
	var XML:JsonDataTypeItem;
	var DATETIME:JsonDataTypeItem;
	var BYTES:JsonDataTypeItem;
	var OBJECT:JsonDataTypeItem;
}

interface UserInfo
{
	id:string;
	username:string;
	created:number;
	modified:number;
	admin_level:string;
	admin_privs:number;
}

interface UserLookupRequest
{
	id?:string;
	username?:string;
}

interface DataItem
{
	key:string;
	value:any;
	data_type?:JsonDataTypeItem;
}

interface DataItemInfo
{
	id:string;
	key:string;
	value:any;
	data_type:JsonDataTypeItem;
	created:number;
	modified:number;
	deleted?:boolean;
}

interface KeyDataRecord
{
	Id:string;
	Type:string;
	Owner:string;
	Parent:string;
	Version:number;
	Created:number;
	Modified:number;
}

interface ChannelMemberInfo
{
	id:string;
	values:{[key:string]:any};
}

interface ChannelInfo
{
	id:string;
	values:{[key:string]:any};
	members:{[key:string]:ChannelMemberInfo};
}

interface HttpResponseInfo
{
	mimeType:string;
	data:number[];
}

declare type DataItemInfoMap = {[key:string]:DataItemInfo};
declare type TransientValueMap = {[key:string]:any};

declare var client:any;
declare var agent:any;
declare var callbacks:any;

declare namespace api
{
	function time():number;
	function makeSVID():string;
	function log_debug(msg:string):void;
	function log_info(msg:string):void;
	function log_warning(msg:string):void;
	function log_error(msg:string):void;
	function getUserInfo(userlookup:UserLookupRequest):UserInfo;
	
	function findKeyDataRecord(id:string):KeyDataRecord;
	function findOrCreateKeyDataRecord(id:string, type:string, owner:string, parent:string):KeyDataRecord;
	function setKeyDataVersion(id:string, version:number):void;
	function deleteKeyData(id:string):void;
	
	function setDataKey(id:string, key:string, value:any):number;
	function setDataKey(id:string, key:string, value:any, data_type:string):number;
	function setDataKeys(id:string, items:DataItem[]):number;
	function getDataKey(id:string, key:string):DataItemInfo;
	function getDataKeys(id:string, keys:string[]):DataItemInfoMap;
	function getDataKeys(id:string, keys:string[], since:number):DataItemInfoMap;
	function getDataKeys(id:string, keys:string[], since:number, includeDeleted:boolean):DataItemInfoMap;
	function getAllDataKeys(id:string):DataItemInfoMap;
	function getAllDataKeys(id:string, since:number):DataItemInfoMap;
	function getAllDataKeys(id:string, since:number, includeDeleted:boolean):DataItemInfoMap;
	function deleteDataKey(id:string, key:string):number;
	function deleteAllDataKeys(id:string):number;

	function getHostWithResident(residentId:string):string;
	function createChannel(channelId:string, residentType?:string, values?:TransientValueMap):string;
	function createGlobalChannel(channelId:string, residentType?:string, values?:TransientValueMap):string;
	function deleteChannel(channelId:string):void;
	function createResident(residentId:string, residentType:string, userId?:string, values?:TransientValueMap):string;
	function deleteResident(residentId:string, finalValues?:TransientValueMap):void;
	function removeResidentFromAllChannels(residentId:string, finalValues?:TransientValueMap):void;
	function setResidentOwner(residentId:string, userId:string):void;
	function addResidentToChannel(channelId:string,residentId:string):void;
	function removeResidentFromChannel(channelId:string,residentId:string, finalValues?:TransientValueMap):void;
	function addChannelListener(channelId:string, userId:string):void;
	function removeChannelListener(channelId:string, userId:string):void;
	function userJoinChannel(userId:string, channelId:string, residentId:string, values?:TransientValueMap):ChannelInfo;
	function userLeaveChannel(userId:string, channelId:string, residentId:string, finalValues?:TransientValueMap):ChannelInfo;
	function getChannelInfo(channelId:string, since:number):ChannelInfo;
	
	function setTransientValue(residentId:string, key:String, value:any):void;
	function setTransientValues(residentId:string, keys:TransientValueMap):void;
	function getTransientValue(residentId:string, key:String):any;
	function getTransientValues(residentId:string, keys:String[]):TransientValueMap;
	function getAllTransientValues(residentId:string):TransientValueMap;
	function deleteTransientValue(residentId:string, key:string):void;
	function deleteTransientValues(residentId:string, keys:string[]):void;
	function deleteAllTransientValues(residentId:string):void;

	function triggerResidentEvent(residentId:string, eventType:string, event:String):void;
	function sendUserMessage(to:string, from:string, fromUser:boolean, guaranteed:boolean, messageType:string, value:string):void;

	function getCurrencyBalance(userId:string, currencyId:string):number;
	function getCurrencyBalances(userId:string):{[currencyId:string]:number};
	function addCurrency(userId:string, currencyId:string, amount:number, reason:string):number;
	function subtractCurrency(userId:string, currencyId:string, amount:number, reason:string):number;

	function base64decode(data:string):number[];
	function base64encode(data:any):string;

	function fileExists(location:string, filename:string):boolean;
	function writeFile(location:string, filename:string, contents:string|number[]):void;
	function readTextFile(location:string, filename:string):string;
	function readBinaryFile(location:string, filename:string):number[];

	function base64decodeAndWriteFile(location:string, filename:string, contents:string):void;

	function getUrlAsString(url:string):string;
	function getUrlAsData(url:string):number[];
	function getUrl(url:string):HttpResponseInfo;
	
}

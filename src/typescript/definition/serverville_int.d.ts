
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

	function createChannel(channelId:string):string;
	function deleteChannel(channelId:string):void;
	function getUserAliasId(userId:string, alias:string):string;
	function userJoinChannel(userId:string, channelId:string, alias:string, values?:TransientValueMap):ChannelInfo;
	function userLeaveChannel(userId:string, channelId:string, alias:string, finalValues?:TransientValueMap):ChannelInfo;
	function getChannelInfo(channelId:string, since:number):ChannelInfo;
	function addResident(channelId:string,residentId:string):void;
	function removeResident(channelId:string,residentId:string, finalValues?:TransientValueMap):void;
	function addListener(userId:string, channelId:string):void;
	function removeListener(userId:string, channelId:string):void;

	function setTransientValue(id:string, key:String, value:any):void;
	function setTransientValues(id:string, keys:TransientValueMap):void;
	function getTransientValue(id:string, key:String):any;
	function getTransientValues(id:string, keys:String[]):TransientValueMap;
	function getAllTransientValues(id:string):TransientValueMap;
	function deleteTransientValue(id:string, key:string):void;
	function deleteTransientValues(id:string, keys:string[]):void;
	function deleteAllTransientValues(id:string):void;
	function sendServerMessage(to:string, from:string, alias:string, messageType:string, value:Object);

	function getCurrencyBalance(userId:string, currencyId:string):number;
	function getCurrencyBalances(userId:string):{[currencyId:string]:number};
	function addCurrency(userId:string, currencyId:string, amount:number, reason:string):number;
	function subtractCurrency(userId:string, currencyId:string, amount:number, reason:string):number;

	function base64decode(data:string):number[];
	function base64encode(data:any):string;

	function writeFile(location:string, filename:string, contents:string|number[]):void;
	function readTextFile(location:string, filename:string):string;
	function readBinaryFile(location:string, filename:string):number[];
}

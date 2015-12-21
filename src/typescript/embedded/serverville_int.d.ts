

interface UserInfo
{
	id:string;
	username:string;
	created:number;
	modified:number;
	admin_level:string;
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
	data_type?:string;
}

interface DataItemInfo
{
	id:string;
	key:string;
	value:any;
	data_type:string;
	created:number;
	modified:number;
	deleted?:boolean;
}

declare type DataItemInfoMap = {[key:string]:DataItemInfo};

declare var client:any;
declare var agent:any;

declare namespace api
{
	function makeSVID():string;
	function log_debug(msg:string):void;
	function log_info(msg:string):void;
	function log_warning(msg:string):void;
	function log_error(msg:string):void;
	function getUserInfo(userlookup:UserLookupRequest):UserInfo;
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
}
/// <reference path="nashorn.d.ts" />
/// <reference path="../definition/serverville_int.d.ts" />

// Javascript to setup a Serverville javascript context

"use strict";

// Holder for exposed client handlers
var client:any = {};

// Holder for exposed agent handlers
var agent:any = {};

// Holder for exposed callback handlers
var callbacks:any = {};

var ValidKeynameRegex:RegExp = new RegExp("^[a-zA-Z_$][0-9a-zA-Z_$]*$");


function isValidKeyname(key:string):boolean
{
	if(key == null)
		return false;
	return ValidKeynameRegex.test(key);
}

class KeyData
{
	id:string;
	record:KeyDataRecord;
	data:any;
	data_info:{[key:string]:DataItemInfo};
	local_dirty:{[key:string]:boolean};
	local_deletes:{[key:string]:boolean};
	most_recent:number;
	dirty:boolean;
	
	constructor(record:KeyDataRecord)
	{
		if(record == null)
			throw "Data must have an database record";
		this.id = record.Id;
		this.record = record;
		this.data = {};
		this.data_info = {};
		this.local_dirty = {};
		this.local_deletes = {};
		this.dirty = false;
		
		this.most_recent = 0;
	}
	
	static find(id:string):KeyData
	{
		var record:KeyDataRecord = api.findKeyDataRecord(id);
		if(record == null)
			return null;
			
		return new KeyData(record);
	}
	
	static findOrCreate(id:string, type:string, owner:string, parent:string=null):KeyData
	{
		var record:KeyDataRecord = api.findOrCreateKeyDataRecord(id, type, owner, parent);
		return new KeyData(record);
	}
	
	static load(id:string, keys?:string[]):KeyData
	{
		var data:KeyData = KeyData.find(id);
		if(data == null)
			return null;
		
		if(keys == undefined)
			data.loadAll();
		else
			data.loadKeys(keys);
		return data;
	}
	
	getId():string { return this.id; }
	getType():string { return this.record.Type; }
	getOwner():string { return this.record.Owner; }
	getParent():string { return this.record.Parent; }
	getVersion():number { return this.record.Version; }
	
	setVersion(version:number):void
	{
		var newVer:number = Math.floor(version);
		if(this.record.Version == newVer)
			return;
			
		this.record.Version = newVer;
		api.setKeyDataVersion(this.id, this.record.Version);
	}
	
	loadKeys(keys:string[]):void
	{
		var vals:{[key:string]:DataItemInfo} = api.getDataKeys(this.id, keys);
		for(var key in vals)
		{
			var dataInfo:DataItemInfo = vals[key];
			this.data_info[key] = dataInfo;
			this.data[key] = dataInfo.value;
		}
	}
	
	loadAll():void
	{
		this.data = {};
		this.local_dirty = {};
		this.dirty = false;
		
		this.data_info = api.getAllDataKeys(this.id);
		for(var key in this.data_info)
		{
			var dataInfo:DataItemInfo = this.data_info[key];
			this.data[key] = dataInfo.value;
			if(dataInfo.modified > this.most_recent)
				this.most_recent = dataInfo.modified;
		}
	}
	
	refresh():void
	{
		this.data_info = api.getAllDataKeys(this.id, this.most_recent, true);
		for(var key in this.data_info)
		{
			var dataInfo:DataItemInfo = this.data_info[key];
			if(dataInfo.deleted)
			{
				delete this.data[key];
			}
			else
			{
				this.data[key] = dataInfo.value;
			}
			
			if(dataInfo.modified > this.most_recent)
				this.most_recent = dataInfo.modified;
		}
	}
	
	set(key:string, val:any, data_type:JsonDataTypeItem = null):void
	{
		if(!isValidKeyname(key))
			throw "Invalid key name: "+key;
			
		if(this.data[key] == val)
			return;
			
		this.data[key] = val;
		var info:DataItemInfo = this.data_info[key];
		if(info)
		{
			info.value = val;
			info.data_type = data_type;
			if(info.deleted)
				delete info.deleted;
		}
		else
		{
			info = {
				"id":this.id,
				"key":key,
				"value":val,
				"data_type":data_type,
				"created":0,
				"modified":0
			};
			this.data_info[key] = info;
		}
		this.dirty = true;
		this.local_dirty[key] = true;
	}
	
	delete(key:string):void
	{
		var info:DataItemInfo = this.data_info[key];
		if(!info)
			return;
		
		delete this.data[key];
		delete this.data_info[key];

		this.dirty = true;
		this.local_deletes[key] = true;
	}

	save():void
	{
		if(this.dirty == false)
			return;
			
		var saveSet:DataItem[] = [];
		
		for(var key in this.local_dirty)
		{
			var info:DataItemInfo = this.data_info[key];

			saveSet.push(
				{
					"key":info.key,
					"value":info.value,
					"data_type":info.data_type
				}
			);
		}
		
		if(saveSet.length > 0)
		{
			api.setDataKeys(this.id, saveSet);
			this.local_dirty = {};
		}

		var deleteSet:string[] = null;
		for(var key in this.local_deletes)
		{
			if(deleteSet == null)
				deleteSet = [];
				
			deleteSet.push(key);
		}

		if(deleteSet && deleteSet.length > 0)
		{
			api.deleteDataKeys(this.id, deleteSet);
			this.local_deletes = {};
		}

		this.dirty = false;
	}
	
	deleteAll():void
	{
		api.deleteKeyData(this.id);
		this.data = {};
		this.data_info = {};
		this.local_dirty = {};
		this.dirty = false;
		
		this.most_recent = 0;
	}
}
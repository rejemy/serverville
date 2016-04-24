/// <reference path="nashorn.d.ts" />
/// <reference path="serverville_int.d.ts" />

// Javascript to setup a Serverville javascript context


delete quit;
delete exit;
delete readLine;
delete print;
delete load;
delete loadWithNewGlobal;
delete Packages;
delete JavaImporter;
delete Java;


// Holder for exposed client handlers
var client:any = {};

// Holder for exposed agent handlers
var agent:any = {};


class KeyData
{
	id:string;
	record:KeyDataRecord;
	data:any;
	data_info:{[key:string]:DataItemInfo};
	local_dirty:{[key:string]:DataItemInfo};
	most_recent:number;
	
	constructor(record:KeyDataRecord)
	{
		if(record == null)
			throw "Data must have an database record";
		this.id = record.Id;
		this.record = record;
		this.data = {};
		this.data_info = {};
		this.local_dirty = {};
		
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
	
	static load(id:string):KeyData
	{
		var data:KeyData = KeyData.find(id);
		if(data == null)
			return null;
		
		data.loadAll();
		return data;
	}
	
	getId():string { return this.id; }
	getType():string { return this.record.Type; }
	getOwner():string { return this.record.Owner; }
	getParent():string { return this.record.Parent; }
	getVersion():number { return this.record.Version; }
	
	setVersion(version:number):void
	{
		this.record.Version = Math.floor(version);
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
		this.local_dirty[key] = info;
	}
	
	save():void
	{
		var saveSet:DataItem[] = [];
		
		for(var key in this.local_dirty)
		{
			var info:DataItemInfo = this.local_dirty[key];

			saveSet.push(
				{
					"key":info.key,
					"value":info.value,
					"data_type":info.data_type
				}
			);
		}
		
		api.setDataKeys(this.id, saveSet);
		
		this.local_dirty = {};
	}
	
	delete():void
	{
		api.deleteKeyData(this.id);
		this.data = {};
		this.data_info = {};
		this.local_dirty = {};
		
		this.most_recent = 0;
	}
}

/// <reference path="serverville_int.d.ts" />

declare class KeyData
{
	id:string;
	data:any;

	static find(id:string):KeyData;
	static findOrCreate(id:string, type:string, owner:string, parent?:string):KeyData;
	static load(id:string):KeyData;
	
	getId():string;
	getType():string;
	getOwner():string;
	getParent():string;
	getVersion():number;
	
	setVersion(version:number):void;
	
	loadKeys(keys:string[]):void;
	loadAll():void;
	
	refresh():void;
	
	set(key:string, val:any, data_type?:JsonDataTypeItem):void;
	
	save():void;
	delete():void;
}
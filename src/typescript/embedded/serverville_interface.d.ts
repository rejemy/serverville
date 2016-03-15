
/// <reference path="serverville_int.d.ts" />

declare class KeyData
{
	id:string;
	data:any;

	constructor(id:string);
	
	static load(id:string):KeyData;
	
	loadAll():void;
	
	refresh():void;
	
	set(key:string, val:any, data_type?:string):void;
	
	save():void;
	
}
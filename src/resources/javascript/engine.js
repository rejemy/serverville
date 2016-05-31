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
"use strict";
// Holder for exposed client handlers
var client = {};
// Holder for exposed agent handlers
var agent = {};
var ValidKeynameRegex = new RegExp("^[a-zA-Z_$][0-9a-zA-Z_$]*$");
function isValidKeyname(key) {
    if (key == null)
        return false;
    return ValidKeynameRegex.test(key);
}
var KeyData = (function () {
    function KeyData(record) {
        if (record == null)
            throw "Data must have an database record";
        this.id = record.Id;
        this.record = record;
        this.data = {};
        this.data_info = {};
        this.local_dirty = {};
        this.dirty = false;
        this.most_recent = 0;
    }
    KeyData.find = function (id) {
        var record = api.findKeyDataRecord(id);
        if (record == null)
            return null;
        return new KeyData(record);
    };
    KeyData.findOrCreate = function (id, type, owner, parent) {
        if (parent === void 0) { parent = null; }
        var record = api.findOrCreateKeyDataRecord(id, type, owner, parent);
        return new KeyData(record);
    };
    KeyData.load = function (id) {
        var data = KeyData.find(id);
        if (data == null)
            return null;
        data.loadAll();
        return data;
    };
    KeyData.prototype.getId = function () { return this.id; };
    KeyData.prototype.getType = function () { return this.record.Type; };
    KeyData.prototype.getOwner = function () { return this.record.Owner; };
    KeyData.prototype.getParent = function () { return this.record.Parent; };
    KeyData.prototype.getVersion = function () { return this.record.Version; };
    KeyData.prototype.setVersion = function (version) {
        var newVer = Math.floor(version);
        if (this.record.Version == newVer)
            return;
        this.record.Version = newVer;
        api.setKeyDataVersion(this.id, this.record.Version);
    };
    KeyData.prototype.loadKeys = function (keys) {
        var vals = api.getDataKeys(this.id, keys);
        for (var key in vals) {
            var dataInfo = vals[key];
            this.data_info[key] = dataInfo;
            this.data[key] = dataInfo.value;
        }
    };
    KeyData.prototype.loadAll = function () {
        this.data = {};
        this.local_dirty = {};
        this.dirty = false;
        this.data_info = api.getAllDataKeys(this.id);
        for (var key in this.data_info) {
            var dataInfo = this.data_info[key];
            this.data[key] = dataInfo.value;
            if (dataInfo.modified > this.most_recent)
                this.most_recent = dataInfo.modified;
        }
    };
    KeyData.prototype.refresh = function () {
        this.data_info = api.getAllDataKeys(this.id, this.most_recent, true);
        for (var key in this.data_info) {
            var dataInfo = this.data_info[key];
            if (dataInfo.deleted) {
                delete this.data[key];
            }
            else {
                this.data[key] = dataInfo.value;
            }
            if (dataInfo.modified > this.most_recent)
                this.most_recent = dataInfo.modified;
        }
    };
    KeyData.prototype.set = function (key, val, data_type) {
        if (data_type === void 0) { data_type = null; }
        if (!isValidKeyname(key))
            throw "Invalid key name: " + key;
        if (this.data[key] == val)
            return;
        this.data[key] = val;
        var info = this.data_info[key];
        if (info) {
            info.value = val;
            info.data_type = data_type;
            if (info.deleted)
                delete info.deleted;
        }
        else {
            info = {
                "id": this.id,
                "key": key,
                "value": val,
                "data_type": data_type,
                "created": 0,
                "modified": 0
            };
            this.data_info[key] = info;
        }
        this.dirty = true;
        this.local_dirty[key] = info;
    };
    KeyData.prototype.save = function () {
        if (this.dirty == false)
            return;
        var saveSet = [];
        for (var key in this.local_dirty) {
            var info = this.local_dirty[key];
            saveSet.push({
                "key": info.key,
                "value": info.value,
                "data_type": info.data_type
            });
        }
        if (saveSet.length > 0)
            api.setDataKeys(this.id, saveSet);
        this.local_dirty = {};
        this.dirty = false;
    };
    KeyData.prototype.delete = function () {
        api.deleteKeyData(this.id);
        this.data = {};
        this.data_info = {};
        this.local_dirty = {};
        this.dirty = false;
        this.most_recent = 0;
    };
    return KeyData;
}());

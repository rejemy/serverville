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
var client = {};
// Holder for exposed agent handlers
var agent = {};
var KeyData = (function () {
    function KeyData(id) {
        if (id == null)
            throw "Data must have an id";
        this.id = id;
        this.data = {};
        this.data_info = {};
        this.local_dirty = {};
        this.most_recent = 0;
    }
    KeyData.load = function (id) {
        var data = new KeyData(id);
        data.loadAll();
        return data;
    };
    KeyData.prototype.loadAll = function () {
        this.data = {};
        this.local_dirty = {};
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
        this.data[key] = val;
        var info = this.data_info[key];
        if (info) {
            info.value = val;
            if (data_type)
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
        this.local_dirty[key] = info;
    };
    KeyData.prototype.save = function () {
        var saveSet = [];
        for (var key in this.local_dirty) {
            var info = this.local_dirty[key];
            saveSet.push({
                "key": info.key,
                "value": info.value,
                "data_type": info.data_type
            });
        }
        api.setDataKeys(this.id, saveSet);
        this.local_dirty = {};
    };
    return KeyData;
})();

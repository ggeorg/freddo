define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/aspect",
        "./DTalkAdapter", 
        "./ServiceMgr"], 

function(declare, lang, aspect, DTalkAdapter, ServiceMgr) {

	return declare("freddo.dtalk.DeviceService", null, {
		_srv: "dtalk.service.Device",
		_srvListener: "$dtalk.service.Device",
		
		info: null,
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
		},
		
		getInfo: function(callback, timeout) {
			if (!this.info) {
				DTalkAdapter.get(this._srv, "info", lang.hitch(this, function(e) {
					if ('result' in e.data) {
						callback.call(this, this.info = e.data.result);
					} else {
						callback.call(this, this.info = null);
					}
				}), this.target, timeout);
			} else {
				callback.call(this, this.info);
			}
		}
	});
	
});
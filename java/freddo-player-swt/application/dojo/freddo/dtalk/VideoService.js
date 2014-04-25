define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/aspect",
        "./DTalkAdapter", 
        "./DTalkService"], 

function(declare, lang, aspect, DTalkAdapter, DTalkService) {

	return declare("freddo.dtalk.VideoService", [DTalkService], {
		_srv: "dtalk.service.Video",
		_srvListener: "$dtalk.service.Video",
		
		_onopenH: null,
		_onpreparedH: null,
		_onCompletionH: null,
		_onErrorH: null,
		_onStatusH: null,
		
		status: null,
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
		},
		
		onStartup: function() {
			this.subscribeToVideoEvents();
		},
		
		subscribeToVideoEvents: function() {			
			this._onPreparedH = DTalkAdapter.subscribeWithCallback(this._srvListener + ".onprepared", lang.hitch(this, function(e) {
				if (this._targetFilter(e.data)) {
					this._onPrepared();
				}
			}), this.target);
			this._onCompletionH = DTalkAdapter.subscribeWithCallback(this._srvListener + ".oncompletion", lang.hitch(this, function(e) {
				if (this._targetFilter(e.data)) {
					this._onCompletion();
				}
			}), this.target);
			this._onErrorH = DTalkAdapter.subscribeWithCallback(this._srvListener + ".onerror", lang.hitch(this, function(e) {
				if (this._targetFilter(e.data)) {
					this._onError();
				}
			}), this.target);
			this._onStatusH = DTalkAdapter.subscribeWithCallback(this._srvListener + ".onstatus", lang.hitch(this, function(e) {
				if (this._targetFilter(e.data)) {
					this._onStatus(e.data.params);
				}
			}), this.target);
		},
		
		_onPrepared: function() {
			this.status = null;
		},
		onPrepared: function(func) {
			return aspect.after(this, "_onPrepared", func, true);
		},
		
		_onCompletion: function() {},
		onCompletion: function(func) {
			return aspect.after(this, "_onCompletion", func, true);
		},
		
		_onError: function() {},
		onError: function(func) {
			return aspect.after(this, "_onError", func, true);
		},
		
		_onStatus: function(status) {
			this.status = status;
		},
		onStatus: function(func) {
			return aspect.after(this, "_onStatus", func, true);
		},
	});
	
});
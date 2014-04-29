define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/aspect",
        "./DTalkAdapter", 
        "./ServiceMgr"], 

function(declare, lang, aspect, DTalkAdapter, ServiceMgr) {

	return declare("freddo.dtalk.DTalkService", null, {
		_srv: null,
		_srvListener: null,
		
		target: null, // read only
		
		timeout: null, // read only
		
		subscribeToEvents: true, // read only

		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
			
			// mix in our passed parameters
			if (params) {
				lang.mixin(this, params);
			}
			
			if (DTalk.getReadyState() == 1) {
				this.startup();
			} else if (!this._onopenH) {
				this._onopenH = DTalk.addEventListener("dtalk.onopen", lang.hitch(this, function() {
					this.startup();
				}));
			}
		},
		
		startup: function() {
			ServiceMgr.start(this._srv, lang.hitch(this, function(e) {
				if ('result' in e.data && e.data.result) {
					this.onStartup();
				} else {
					this.onStartupError();
				}
			}), this.target, this.timeout);
		},
		
		onStartup: function() {
			
		},
		
		onStartupError: function() {
			
		},
		
		get: function(property, callback) {
			DTalkAdapter.get(this._srv, property, callback, this.target, this.timeout);
		},
		
		set: function(params) {
			DTalkAdapter.set(this._srv, params, this.target);
		},
		
		invoke: function(action, params, callback) {
			if (callback) {
				DTalkAdapter.invokeWithCallback(this._srv, action, params, callback, this.target);
			} else {
				DTalkAdapter.invoke(this._srv, action, params, this.target);
			}
		},
		
		subscribe: function(event, callback) {
			DTalkAdapter.subscribeWithCallback(this._srvListener + "." + event, callback, this.target, this.timeout);
		},
		
		_targetFilter: function(message) {
			return ((this.target && this.target === message.from) || (!this.target && !message.from));
		}

	});

});
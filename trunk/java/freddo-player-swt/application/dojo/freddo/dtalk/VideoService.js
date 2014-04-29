define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/aspect",
        "./DTalkAdapter", 
        "./DTalkService"], 

function(declare, lang, aspect, DTalkAdapter, DTalkService) {

	return declare("freddo.dtalk.VideoService", [ DTalkService ], {
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
			var videoService = this;
			
			if (videoService.subscribeToEvents) {
				videoService._subscribeToEvents();
			}
		},
		
		_subscribeToEvents: function() {
			var videoService = this;
			
			videoService._onPreparedH = videoService.subscribe("onprepared", function(e) {
				if (videoService._targetFilter(e.data)) {
					videoService._onPrepared();
				}
			});
			
			videoService._onCompletionH = videoService.subscribe("oncompletion", function(e) {
				if (videoService._targetFilter(e.data)) {
					videoService._onCompletion();
				}
			});

			videoService._onErrorH = videoService.subscribe("onerror", function(e) {
				if (videoService._targetFilter(e.data)) {
					videoService._onError();
				}
			});
			
			videoService._onStatusH = videoService.subscribe("onstatus", function(e) {
				if (videoService._targetFilter(e.data)) {
					videoService._onStatus(e.data.params);
				}
			});
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
		
		getSrc: function(callback) {
			this.get("src", callback);
		},
		
		setSrc: function(src) {
			this.set({src: src});
		},
		
		play: function() {
			this.invoke("play");
		},
		pause: function() {
			this.invoke("pause");
		},
		stop: function() {
			this.invoke("stop");
		},
		seekTo: function(sec) {
			this.invoke("seekTo", sec);
		}
	});
	
});
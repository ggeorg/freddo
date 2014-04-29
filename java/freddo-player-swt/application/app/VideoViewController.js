define(["dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/dom",
        "dojo/dom-class",
        "dojo/query",
        "dojo/aspect",
        "dijit/registry",
        "freddo/dtalk/VideoService", 
        "app/ViewController",
        "dojo/NodeList",
        "dojo/NodeList-dom"], 
        
function(declare, lang, dom, domClass, query, aspect, registry, VideoService, ViewController) {
	
	return declare("app.VideoViewController", [ViewController], {
		id: "freddo-tv-video-view",
		
		videoService: null,
		videoControls: null,

		_onVisibilityChangedH: null,
		
		_onErrorH: null,
		_onCompletionH: null,
		_onStatusH: null,
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
			
			if (DTalk.getReadyState() === 1) {
				this._createVideoService();
			} else {
				DTalk.addEventListener("dtalk.onopen", lang.hitch(this, function() {
					this._createVideoService();
				}));
			}
		},
		
		_createVideoService: function() {
			this.videoService = new VideoService(/*{target: "lala"}*/);
			this.videoControls = registry.byId("videoControls");
			this.videoControls.set("video", this.videoService);
			
			if (!this._onVisibilityChangedH) {
				this._onVisibilityChangedH = this.videoControls.onVisibilityChanged(lang.hitch(this, function(visible) {
					this.onVisibilityChanged(visible);
				}));
			}
			if (!this._onErrorH) {
				this._onErrorH = this.videoService.onError(lang.hitch(this, function() {
					this.onError();
				}));
			}
			if (!this._onCompletionH) {
				this._onCompletionH = this.videoService.onCompletion(lang.hitch(this, function() {
					this.onCompletion();
				}));
			}
			if (!this._onStatusH) {
				this._onStatusH = this.videoService.onStatus(lang.hitch(this, function(status) {
					this.onStatus(status);
				}));
			}
		},
		
		startView: function() {
			this.afterTransitionIn();
		},
		
		afterTransitionIn: function() {
			var videoViewController = this;
			
			videoViewController.mouseDownH = videoViewController.view.on("click", function() {
				if (videoViewController.videoControls.status && videoViewController.videoControls.status.paused) {
					return;
				}
				videoViewController._clearVideoControlsTimeout(true);
			});
			
			videoViewController._clearVideoControlsTimeout(true);
		},
		
		beforeTransitionOut: function() {
			var videoViewController = this;
			
			if (videoViewController.mouseDownH) {
				videoViewController.mouseDownH.remove();
				videoViewController.mouseDownH = null;
			}
			
			videoViewController._clearVideoControlsTimeout(false);
		},
		
		_clearVideoControlsTimeout: function(renew) {
			var videoViewController = this;
			
			if (videoViewController.videoControlsTimeout) {
				clearTimeout(videoViewController.videoControlsTimeout);
				videoViewController.videoControlsTimeout = null;
			}
			
			if (renew) {
				if (!domClass.contains("videoControls", "visible")) {
					domClass.add("videoControls", "visible");
				}
				
				videoViewController.videoControlsTimeout = setTimeout(function() {
					videoViewController.videoControlsTimeout = null;
					if (videoViewController.videoControls.status && videoViewController.videoControls.status.paused) {
						return;
					}
					domClass.remove("videoControls", "visible");
				}, 7333);
			} else if (domClass.contains("videoControls", "visible")) {
				domClass.remove("videoControls", "visible");
			}
		},
		
		onVisibilityChanged: function(visible) {
			visible ? query("#videoControls").addClass("visible") : query("#videoControls").removeClass("visible");
		},
		
		onError: function() {
			if (this.isActive()) {
				//this.videoControls.onError();
				this.deactivate("fade");
			}
		},
		
		onCompletion: function() {
			if (this.isActive()) {
				//this.videoControls.onCompletion();
				this.deactivate("fade");
			}
		},
		
		onStatus: function(status) {
			if (!this.isActive()) {
				this.activate("fade");
			}
			this.videoControls.onStatus(status);
		}
		
	});

});
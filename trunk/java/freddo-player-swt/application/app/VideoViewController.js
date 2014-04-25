define(["dojo/_base/declare", 
        "dojo/_base/lang",
        "freddo/dtalk/VideoService", 
        "app/ViewController"], 
        
function(declare, lang, VideoService, ViewController) {
	
	return declare("app.VideoViewController", [ViewController], {
		id: "video",
		
		videoService: null,

		_onErrorH: null,
		_onCompletionH: null,
		_onStatusH: null,
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
			this.videoService = new VideoService(/*{target: "lala"}*/);
			
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
		
		onError: function() {
			if (this.isActive()) {
				this.deactivate("fade");
			}
		},
		
		onCompletion: function() {
			if (this.isActive()) {
				this.deactivate("fade");
			}
		},
		
		onStatus: function() {
			if (!this.isActive()) {
				this.activate("fade");
			}
		}
		
	});

});
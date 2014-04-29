define([ "dojo/_base/declare", 
         "dojo/_base/lang", 
         "dojo/_base/array",
         "dojo/query",
         "dojo/on", 
         "dojo/aspect", 
         "dojo/touch",
         "dijit/registry",
		 "dijit/_WidgetBase",
		 "dijit/_TemplatedMixin",
		 "dijit/_WidgetsInTemplateMixin",
		 "dojo/text!./templates/VideoControls.html",
		 "freddo/dtalk/PresenceService",
		 "dojox/mobile/Slider",
		 "dojox/mobile/IconMenu",
		 "dojox/mobile/IconMenuItem",
		 "dojo/NodeList-dom"],
         
function(declare, lang, array, query, on, aspect, touch, registry, WidgetBase, TemplateMixin, WidgetsInTemplateMixin, template, PresenceService) {
	
	if (!('toHHMMSS' in String.prototype)) {
		String.prototype.toHHMMSS = function() {
			var sec_num = parseInt(this, 10); // don't forget the second param
			var hours = Math.floor(sec_num / 3600);
			var minutes = Math.floor((sec_num - (hours * 3600)) / 60);
			var seconds = sec_num - (hours * 3600) - (minutes * 60);

			if (hours < 10) {
				hours = "0" + hours;
			}
			if (minutes < 10) {
				minutes = "0" + minutes;
			}
			if (seconds < 10) {
				seconds = "0" + seconds;
			}
			var time = hours + ':' + minutes + ':' + seconds;
			return time;
		};
	}
	
	return declare("freddo.mobile.VideoControls", [ WidgetBase, TemplateMixin, WidgetsInTemplateMixin ], {
		templateString: template,
		
		_playIcon: "css/dojo/freddo/mobile/video-controls/icons/media-play.png",
		_pauseIcon: "css/dojo/freddo/mobile/video-controls/icons/media-pause.png",
		
		video: null,
		_status: null,
		_onSlider: false,
		
		presence: null,
		
		baseClass: "freddo-mobile-video-controls",
		
		postCreate : function() {
			var videoControls = this;
			
			videoControls.inherited(arguments);
			
			touch.press(this.slider.domNode, function(event) {
				if (videoControls.video) {
					videoControls._onSlider = true;
				}
			}, true);
			
			touch.release(videoControls.slider.domNode, function(event) {
				if (videoControls.video) {
					if (videoControls.status && videoControls.status.duration && videoControls.status.duration > 0) {
						var sec = (videoControls.slider.get("value") * videoControls.status.duration) / 100.0;
						videoControls.video.seekTo(Math.max(0.1, Math.min(sec, videoControls.status.duration - 0.1)));
					}
					videoControls._onSlider = false;
				}
			}, true);
			
			videoControls.mediaPlayPauseBtn.on("click", function(event) {
				if (videoControls.video) {
					if (videoControls.mediaPlayPauseBtn.icon === videoControls._playIcon) {
						videoControls.video.play();
					} else {
						videoControls.video.pause();
					}
				}
			});
			
			videoControls.mediaStopBtn.on("click", function(event) {
				if (!videoControls.video) {
					videoControls.video.stop();
				}
			});
			
			videoControls.mediaSeekBackwardBtn.on("click", function(event) {
				if (videoControls.video) {
					if (videoControls.status && videoControls.status.position && videoControls.status.position >= 0) {
						videoControls.video.seekTo(Math.max(0.1, videoControls.status.position - 15.5));
					}
				}
			});
			
			videoControls.mediaSeekForwardBtn.on("click", function(event) {
				if (videoControls.video) {
					if (videoControls.status && videoControls.status.position && videoControls.status.position >= 0) {
						videoControls.video.seekTo(Math.max(0, videoControls.status.position + 14.5));
					}
				}
			});
			
			videoControls.mediaPushToBtn.on("click", function(event) {
				if (!videoControls.presence) {
					try {
					videoControls.presence = new PresenceService();
					videoControls.deviceList.setStore(videoControls.presence.roster);
					//videoControls.deviceList.refresh();
					}catch(e){alert(e);}
				}
				
				// if videoControls.presence
				videoControls.deviceListDlg.show();
			});
			
			videoControls.deviceList.on("click", function(e) {
				var listItem = registry.getEnclosingWidget(e.target);
				if (listItem) {
					var item = videoControls.presence.roster.get(listItem.name);
					if (item) {
						alert(JSON.stringify(item));
					}
				}
			});
		},
		
//		onError: function() {
//			
//		},
//		
//		onCompletion: function() {
//			
//		},
		
		onStatus: function(status) {
			if (!this.video) {
				//this.itemTitle.innerHTML = "No devices found!";
				//this.itemSubTitle.innerHTML = "scanning...";
				this.slider.set("value", 0);
				this.position.innerHTML = "00:00:00";
				this.duration.innerHTML = "00:00:00";
				return;
			}
			
			// keep old paused state...
			var oldPaused = this.status ? this.status.paused : false;
			
			this.status = status;
			
			if (this.status && this.status.src) {
				// this.itemTitle.innerHTML = this.status.title || "[No
				// Title]";
				//this.itemSubTitle.innerHTML = "&nbsp;"; // this.status.src;
			} else {
				//this.itemTitle.innerHTML = this._getName(this.video.target);
				//this.itemSubTitle.innerHTML = "not active";
			}
			
			if (!this._onSlider) {
				this.slider.set("value", this._getProgress());
			}
			this.position.innerHTML = this.status && 'position' in this.status ? this.status.position.toString().toHHMMSS() : "00:00:00";
			this.duration.innerHTML = this.status && 'duration' in this.status ? this.status.duration.toString().toHHMMSS() : "00:00:00";

			this.mediaPlayPauseBtn.set("label", this.status.paused ? "Play" : "Pause");
			this.mediaPlayPauseBtn.set("icon", this.status.paused ? this._playIcon : this._pauseIcon);
			
			if (oldPaused != this.status.paused) {
				this._onVisibilityChanged(this.status.paused);
			}
		},
		
		_onVisibilityChanged: function(e) {},
		onVisibilityChanged: function(func) {
			return aspect.after(this, "_onVisibilityChanged", func, true);
		},
		
		_getProgress : function() {
			var position = this.status.position && !isNaN(this.status.position) ? this.status.position : 0;
			var duration = this.status.duration && !isNaN(this.status.duration) ? this.status.duration : this.status.position;
			return Math.max(0, Math.min((position > 0) ? 100 * (position / duration) : 0));
		},
		
	});
	
});
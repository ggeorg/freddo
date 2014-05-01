(function() {
	var DTalkVideoService = {
		name: "dtalk.service.Video",
		_video: null,
		_timerH: null,
		_startPositionPercent: 0,
		init: function() {
			this._video = document.querySelector("#x-dtalk-video");
			if (!this._video) {
				this._video = document.createElement("video");
				this._video.id = "x-dtalk-video";
				this._video.style.position = "absolute";
				this._video.style.top = this._video.style.left = "0px";
				this._video.style.width = this._video.style.height = "100%";
				this._video.style.zIndex = -9999;
				
				var body = document.querySelector("body");
				body.appendChild(this._video);
				
				var self = this;
				
				this._video.addEventListener("loadedmetadata", function(e) {
					//alert("Event: loadedmetadata");
					
					if (self._startPositionPercent > 0 && self._startPositionPercent < 1) {
						try {
							self._video.currentTime = self._startPositionPercent * self._video.duration;
						} catch(e) {
							console.error(e);
						}
						self._startPositionPercent = 0;
					}
					fireEvent("onprepared");
				});
				
				this._video.addEventListener("ended", function(e) {
					//alert("Event: ended");
					
					self._stopTimer();
					fireEvent("oncompletion");
				});
				
				this._video.addEventListener("error", function(e) {
					var msg;
					switch (e.target.error.code) {
					case e.target.error.MEDIA_ERR_ABORTED:
						msg = 'You aborted the video playback.';
						break;
					case e.target.error.MEDIA_ERR_NETWORK:
						msg = 'A network error caused the video download to fail part-way.';
						break;
					case e.target.error.MEDIA_ERR_DECODE:
						msg = 'The video playback was aborted due to a corruption problem or because the video used features your browser did not support.';
						break;
					case e.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED:
						msg = 'The video could not be loaded, either because the server or network failed or because the format is not supported.';
						break;
					default:
						msg = 'An unknown error occurred.';
						break;
					}
					console.debug("Error: " + msg);
					fireEvent("onerror", msg);
					self._stopTimer();
				});
			}
		},
		get_src: function(request) {
			sendResponse(request, this._video.src);
		},
		set_src: function(event) {
			var src = event.params.src;
			if (src) {
				this._video.src = src;
				this._video.load();
				this._startTimer();
			}
		},
		set_startPositionPercent: function(event) {
			var value = event.params.startPositionPercent;
			if (!isNaN(value)) {
				this._startPositionPercent = value;
			}
		},
		get_info: function(request) {
			sendResponse(request, this._getStatus());
		},
		do_play: function() {
			this._video.play();
			this._sendStatus();
			this._startTimer();
		},
		do_stop: function() {
			this._video.pause();
			this._stopTimer();
			fireEvent("oncompletion");
		},
		do_pause: function() {
			this._video.pause();
			this._sendStatus();
		},
		do_seekTo: function(event) {
			this._video.currentTime = event.params;
			this._sendStatus();
		},
		do_setRate: function(event) {
			var value = event.params;
			if (!isNaN(value)) {
				if (value === 0) {
					this.do_pause();
				} else {
					this.do_play();
				}
			}
		},
		_getStatus: function() {
			var status = {
				src: this._video.src,
				duration: this._video.duration,
				position: this._video.currentTime,
				paused: this._video.paused
			};
			// TODO: volume, canPause, bufferedPercent
			return status;
		},
		_sendStatus: function() {
			fireEvent("onstatus", this._getStatus());
		},
		_startTimer: function() {
			var self = this;
			if (!self.timerH) {
				self.timerH = setInterval(function() {
					self._sendStatus();
				}, 1000);
			}
			// else: reuse old timer  
		},
		_stopTimer: function() {
			if (this.timerH) {
				this._sendStatus();
				clearInterval(this.timerH);
				this.timerH = null;
			}
		}
	};
		
	function fireEvent(event, data) {
		var evt = {
			dtalk: "1.0",
			service: "$dtalk.service.Video." + event
		};
		if (data) {
			evt.params = data;
		}
		DTalk.sendNotification(evt);
	}
	
	function newResponse(req) {
		var res = null;
		if ("id" in req) {
			res = {
				dtalk: "1.0",
				service: req.id
			};
			// if ("from" in req) {
			// res.to = req.from;
			//					}
		}
		return res;
	}
	
	function sendResponse(req, data) {
		var res = newResponse(req);
		if (res) {
			res.result = data;
			DTalk.sendNotification(res);
		}
	}

	function init() {
		var service = DTalkVideoService;
		
		DTalk.addEventListener(service.name, function(e) {
			if (e && e.data && e.data.action) {
				var action = e.data.action;
				switch(action) {
				case "get":
					var getter = "get_" + e.data.params;
					if (getter in service) {
						service[getter].call(service, e.data);
					}
					break;
				case "set":
					var properties = e.data.params;
					for (var p in properties) {
						var setter = "set_" + p;
						if (setter in service) {
							service[setter].call(service, e.data);
						}
					}
					break;
				default:
					var method = "do_" + e.data.action;
					if (method in service) {
						service[method].call(service, e.data);
					}
					break;
				}
			}
		}, true);
		
		service.init();
	}

	if (window.DTalk) {
		if (DTalk.getReadyState() == 1) {
			init();
		} else {
			DTalk.addEventListener("dtalk.onopen", function(e) {
				init();
			}, false);
		}
	} else {
		alert("DTalk missing!");
	}
})();
define(["dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/dom",
        "dojo/query",
        "dojo/on",
        "dijit/registry",
        "freddo/dtalk/VideoService",
        "app/ViewController"], 
        
function(declare, lang, dom, query, on, registry, VideoService, ViewController) {
	
	function handleDragEnter(e) {
		// Stops some browsers from redirecting.
		e.stopPropagation();
		e.preventDefault();
		
		// visual indicator
		query("#main-drop-zone").addClass("dragover");
	}
	
	function handleDragLeave(e) {
		// Stops some browsers from redirecting.
		e.stopPropagation();
		e.preventDefault();
		
		// visual indicator
		query("#main-drop-zone").removeClass("dragover");
	}
	
	function handleDragOver(e) {
		// Stops some browsers from redirecting.
		e.stopPropagation();
		e.preventDefault();
		
		 // Explicitly show this is a copy.
		e.dataTransfer.dropEffect = 'copy';
	}
	
	function handleDrop(mainViewController, e) {
		// Stops some browsers from redirecting.
		e.stopPropagation();
		e.preventDefault();
		
		// Loop through the FileList...
		var files = e.dataTransfer.files;
		for (var i = 0, f; f = files[i]; i++) {
			// Only process HTML files.
			if (!f.type.match("text/html")) {
				continue;
			}
			
			var reader = new FileReader();
			reader.onload = (function(theChannel) {
				return function(e) {
					var channelViewContainer = registry.byId("freddo-tv-channel-view-container"); 
					try {
						channelViewContainer.set("content", e.target.result);
					} catch (e) {
						alert(e);
						
						if (!mainViewController.showChannelBtnH) {
							mainViewController.showChannelBtnH.remove();
							mainViewController.showChannelBtnH = null;
							query("#freddo-tv-main-view-show-channel-btn").style("display", "none");
						}
						
						channelViewContainer.set("content", "");
						return;
					}

					if (!mainViewController.showChannelBtnH) {
						window.setTimeout(function() {
							mainViewController.showChannelBtnH = query("#freddo-tv-main-view-show-channel-btn")
							.style("display", "")
							.on("click", function() {
									mainViewController.activateView("freddo-tv-channel-view", "fade");
								});
						}, 3333);
					}
					
					mainViewController.activateView("freddo-tv-channel-view", "fade");
				}
			})(f);
			
			// Read the file...
			reader.readAsText(f);
			
			// don't process any other file.
			break;
		}
		
		// visual indicator
		query("#main-drop-zone").removeClass("dragover");
	}
	
	return declare("app.MainViewController", [ViewController], {
		id: "freddo-tv-main-view",
		
		dragenterH: null,
		dragleaveH: null,
		drapoverH: null,
		dropH: null,
		
		showChannelBtnH: null,
		
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
		},
		
		startView: function() {
			this.afterTransitionIn();
		},
		
		beforeTransitionIn: function() {
			var mainViewController = this;
			
			if (mainViewController.videoService) {
				mainViewController.videoService.getSrc(function(e) {
					if ("result" in e.data && e.data.result && e.data.result.length > 0) {
						if (!mainViewController.replayBtnH) {
							mainViewController.replayBtnH = query("#freddo-tv-main-view-replay-btn")
								.style("display", "")
								.on("click", function() {
										setTimeout(function() {
											mainViewController.videoService.play();
										}, 333);
									});
						}
					} else {
						if (mainViewController.replayBtnH) {
							mainViewController.replayBtnH.remove();
							mainViewController.replayBtnH = null;
							query("#freddo-tv-main-view-replay-btn").style("display", "none")
						}
					}
				});
			}
		},
		
		afterTransitionIn: function() {
			var mainViewController = this;
			
			mainViewController.addDNDListeners();
			
			mainViewController.infoBtnH = query("#freddo-tv-main-view-info-btn")
				.on("click", function() {
						alert("TODO");
					});
		},
		
		beforeTransitionOut: function() {
			var mainViewController = this;
			
			mainViewController.removeDNDListeners();
			
			if (mainViewController.infoBtnH) {
				mainViewController.infoBtnH.remove();
				mainViewController.infoBtnH = null;
			}
		},
		
		addDNDListeners: function() {
			var mainViewController = this;
			
			var dropZone = dom.byId("main-drop-zone");
			// TODO check
			
			this.drapenterH = on(dropZone, "dragenter", function(e) {
				handleDragEnter(e);
			});
			this.drapleaveH = on(dropZone, "dragleave", function(e) {
				handleDragLeave(e);
			});
			this.drapoverH = on(dropZone, "dragover", function(e) {
				handleDragOver(e);
			});
			this.dropH = on(dropZone, "drop", function(e) {
				handleDrop(mainViewController, e);
			});
		},
		
		removeDNDListeners: function() {
			if (this.dragenterH) {
				this.dragenterH.remove();
				this.dragenterH = null;
			}
			
			if (this.dragleaveH) {
				this.dragleaveH.remove();
				this.dragleaveH = null;
			}
			
			if (this.dropoverH) {
				this.dropoverH.remove();
				this.dropoverH = null;
			}
			
			if (this.dropH) {
				this.dropH.remove();
				this.dropH = null;
			}
		}
	});

});
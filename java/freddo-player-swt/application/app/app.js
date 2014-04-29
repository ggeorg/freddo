define(["dojo/query",
        "dojo/on",
        "dojo/dom",
        "dojox/mobile/parser",
        "app/MainViewController",
        "app/VideoViewController",
        "app/ChannelViewController",
        "dojo/NodeList-dom"], 
function(query, on, dom, parser, MainViewController, VideoViewController, ChannelViewController) {
	
	return {
		mainViewCtrl: null,
		videoViewCtrl: null,
		channelViewCtrl: null,
		
		init: function() {
			var app = this;
			
			// disable drop in main area...
			on(document, "body:dragover", function(e) {
				e.stopPropagation();
				e.preventDefault();
			});
			on(document, "body:drop", function(e) {
				e.stopPropagation();
				e.preventDefault();
			});
			
			// disable context menu...
			on(document, "contextmenu", function(e) {
				//e.stopPropagation();
				//e.preventDefault();
			});
			
			parser.parse();
			
			// make UI visible...
			query("#loadDiv").style("display", "none");
			
			// create view controllers...
			app.mainViewCtrl  = new MainViewController();
			app.videoViewCtrl = new VideoViewController();
			app.channelViewCtrl = new ChannelViewController();
			
			// DTalk initialization...
			
			DTalk.onopen = function() {
				var label = document.getElementById("main-ws-label");
				label.innerHTML = DTalk.getURLParam("ws");
				
				// Check for the various File API support.
				if (window.File && window.FileReader && window.FileList && window.Blob) {
					// Great success! All the File APIs are supported.
				} else {
					alert('The File APIs are not fully supported in this browser.');
				}
			};
			
			DTalk.onclose = function() {
				//alert('onclose');
			};
			
			DTalk.connect();
		}
	};
	
})
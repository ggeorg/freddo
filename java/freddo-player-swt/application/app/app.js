define(["dojox/mobile/parser",
        "app/MainViewController",
        "app/VideoViewController"], 
function(parser, MainViewController, VideoViewController) {
	
	return {
		mainViewCtrl: null,
		videoViewCtrl: null,
		
		init: function() {
			var app = this;
			
			parser.parse();
			
			// TODO  make UI visible...
			
			app.mainViewCtrl  = new MainViewController();
			app.videoViewCtrl = new VideoViewController();
			
			// DTalk initialization...
			
			DTalk.onopen = function() {
				var label = document.getElementById("main-ws-label");
				label.innerHTML = DTalk.getURLParam("ws");
			};
			
			DTalk.onclose = function() {
				//alert('onclose');
			};
			
			DTalk.connect();
		}
	};
	
})
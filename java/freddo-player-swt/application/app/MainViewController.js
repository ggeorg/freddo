define(["dojo/_base/declare", "app/ViewController"], function(declare, ViewController) {
	
	return declare("app.MainViewController", [ViewController], {
		id: "main",
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
		},
		
		startView: function() {
			//alert("===========");
		}
		
	});

});
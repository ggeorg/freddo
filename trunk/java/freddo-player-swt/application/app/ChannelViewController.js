define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dijit/registry",
        "app/ViewController"], 
        
function(declare, lang, registry, ViewController) {
		
	return declare("app.ChannelViewController", [ViewController], {
		id: "freddo-tv-channel-view",
		
		doneBtnH: null,
		
		postscript: function(/*Object?*/ params) {
			this.inherited(arguments);
		},
		
		afterTransitionIn: function() {
			var doneToolBarBtn = registry.byId("freddo-tv-channel-view-done-toolbar-btn");
			this.doneBtnH = doneToolBarBtn.on("click", lang.hitch(this, function() {
				this.deactivate("fade");
			}));
		},
		
		beforeTransitionOut: function() {
			if (this.doneBtnH) {
				this.doneBtnH.remove();
				this.doneBtnH = null;
			}
		}
	});

});
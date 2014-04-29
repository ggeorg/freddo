define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dijit/registry"], function(declare, lang, registry) {
	
	return declare("app.ViewController", null, {
		id: null,
		view: null,
		
		_startViewH: null,
		_beforeTransitionInH: null,
		_afterTransitionInH: null,
		_beforeTransitionOutH: null,
		_afterTransitionOutH: null,
		
		_viewStack: [],
		
		postscript: function(/*Object?*/ params) {
			this.view = registry.byId(this.id);

			this.view.on("startView", lang.hitch(this, function() {
				this.startView();
			}));
			
			this.view.on("beforeTransitionIn", lang.hitch(this, function() {
				//alert("beforeTransitionIn:"+this.view.id);
				this.beforeTransitionIn();
			}));
			
			this.view.on("afterTransitionIn", lang.hitch(this, function() {
				//alert("afterTransitionIn:"+this.view.id);
				this.afterTransitionIn();
			}));
			
			this.view.on("beforeTransitionOut", lang.hitch(this, function() {
				//alert("beforeTransitionOut:"+this.view.id);
				this.beforeTransitionOut();
			}));
			
			this.view.on("afterTransitionOut", lang.hitch(this, function() {
				//alert("afterTransitionOut:"+this.view.id);
				this.afterTransitionOut();
			}));
		},
		
		startView: function() {
			// does nothing..
		},
		
		beforeTransitionIn: function() {
			// does nothing..
		},
		
		afterTransitionIn: function() {
			// does nothing..
		},
		
		beforeTransitionOut: function() {
			// does nothing..
		},
		
		afterTransitionOut: function() {
			// does nothing..
		},
		
		performTransition: function(/*String*/moveTo, /*Number*/transitionDir, /*String*/transition) {
			this.view.getShowingView().performTransition(moveTo, transitionDir, transition);
		},
		
		isActive: function() {
			return this.view === this.view.getShowingView();
		},
		
		activate: function(/*String*/transition) {
			var showingView = this.view.getShowingView();
			showingView.performTransition(this.view.id, 1, transition);
			this._viewStack.push(showingView);
		},
		
		activateView: function(/*String*/viewId, /*String*/transition) {
			var showingView = this.view.getShowingView();
			showingView.performTransition(viewId, 1, transition);
			this._viewStack.push(showingView);
		},
		
		deactivate: function(/*String*/transition) {
			var view = this._viewStack.pop();
			if (view) {
				this.view.getShowingView().performTransition(view.id, -1, transition);
			}
		}
	});

});
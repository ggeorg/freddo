define(["dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/_base/array",
        "dojo/aspect",
        "dojo/store/Memory", 
        "dojo/store/Observable", 
        "./DTalkAdapter", 
        "./DTalkService"], 

function(declare, lang, array, aspect, Memory, Observable, DTalkAdapter, DTalkService) {
	
	return declare("freddo.dtalk.PresenceService", [ DTalkService ], {
		_srv: "dtalk.service.Presence",
		_srvListener: "$dtalk.service.Presence",
		
		roster: null,
		
		postscript: function(/*Object?*/ params) {
			var presenceService = this;
			
			presenceService.roster = new Observable(new Memory({
				idProperty: "name",
				data: []
			}));
			
			presenceService.inherited(arguments);
		},
		
		onStartup: function() {
			var presenceService = this;
			
			if (presenceService.subscribeToEvents) {
				presenceService._subscribeToEvents();
				
				presenceService.getRoster(function(evt) {
					try {
						//alert(JSON.stringify(evt.data));
						
						if (evt && evt.data && evt.data.result) {
							array.forEach(evt.data.result, function(item) {
								try{
								//alert(JSON.stringify(item));
								
								//if (presenceService._filter(item)) {
									item.href = "#";
									item.displayName = presenceService._mkDisplayName(item.name);
									presenceService.roster.put(item, {
										overwrite: true
									});
								//}
								}catch(e){alert(e);}
							});
						}
					} catch(e) {
						console.error(e);
					}
				});
			}
		},
		
//		_filter : function(item) {
//			return item && ("dtype" in item) && item.dtype.contains && item.dtype.contains("Renderer/1");
//		},
		
		_mkDisplayName : function(name) {
			var idx = name.indexOf("@");
			return ((idx !== -1) ? name.slice(0, idx) : name).replace("'", "&#39;").replace('"', '&#34;');
		},
		
		_subscribeToEvents: function() {
			var presenceService = this;
			
			presenceService._onResolvedH = presenceService.subscribe("onresolved", function(e) {
				try {
					if (presenceService._targetFilter(e.data)) {
						presenceService._onResolved(e);
					}
				} catch(e) {
					console.error(e);
				}
			});
			
			presenceService._onRemovedH = presenceService.subscribe("onremoved", function(e) {
				try {
					if (presenceService._targetFilter(e.data)) {
						presenceService._onRemoved(e);
					}
				} catch(e) {
					console.error(e);
				}
			});
		},
		
		_onResolved: function(evt) {
			var presenceService = this;
			try{
			if (evt && evt.data && evt.data.params) {
				var item = evt.data.params;
				//alert(JSON.stringify(item));
				//if (presenceService._filter(item)) {
					item.href = "#";
					item.displayName = presenceService._mkDisplayName(item.name);
					presenceService.roster.put(item, {
						overwrite : true
					});
				//}
			}
			}catch(e){alert(e);}
		},
		onResolved: function(func) {
			return aspect.after(this, "_onResolved", func, true);
		},
		
		_onRemoved: function(evt) {
			var presenceService = this;
			
			if (evt && evt.data && evt.data.params) {
				var name = evt.data.params.name;
				if (presenceService.roster.get(name)) {
					presenceService.roster.remove(name);
				}
			}
		},
		onRemoved: function(func) {
			return aspect.after(this, "_onRemoved", func, true);
		},
		
		getRoster: function(callback) {
			this.get("roster", callback);
		}
		
	});
	
});
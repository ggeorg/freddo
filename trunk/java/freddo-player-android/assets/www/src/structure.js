define(["dojo/_base/lang"], function(lang) {
	lang.getObject("src.structure", true);
	
	var THRESHOLD_WIDTH = 600;
	
	src.structure = {
		layout: {
			threshold: THRESHOLD_WIDTH, // threshold for layout change
			leftPane: {
				hidden: (window.innerWidth < THRESHOLD_WIDTH) ? true : false,
				currentView: null
			},
			rightPane: {
				hidden: false,
				currentView: null
			},
			getViewHolder: function(id) {
				if (id === "navigation")
					return (window.innerWidth < THRESHOLD_WIDTH) ? "rightPane" : "leftPane";
				else
					return "rightPane";
			},
			setCurrentView: function(id) {
				var holder = this.getViewHolder(id);
				this[holder].currentView = id;
			},
			current: {
				id: "welcome",
				title: "Welcome"
			}
		},
		applets: [{
			id: "general",
			label: "General",
			views: [{
				id: "device",
				title: "Device",
				url: "views/device.html",
				jsmodule: "src/device.js"
			}]
		}],
		/* Below are internal views. */
		_views: [{
			id: 'welcome',
			title: 'Welcome'
		}, {
			id: 'navigation',
			title: 'Showcase',
			type: 'navigation',
			back: ''
		}],
		/* Data model for tracking view loading */
		load: {
			loaded: 0,
			target: 0 // target number of views that should be loaded
		},
		/* Navigation list */
		navRecords: []
	};
	
	return src.structure;
});
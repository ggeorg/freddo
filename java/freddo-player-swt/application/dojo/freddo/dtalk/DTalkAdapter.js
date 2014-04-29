define([], function(declare) {

	var DTalkAdapter = {
		KEY_FROM: "from",
		KEY_TO: "to",
		KEY_BODY: "body",
		KEY_BODY_VERSION: "dtalk",
		KEY_BODY_ID: "id",
		KEY_BODY_SERVICE: "service",
		KEY_BODY_ACTION: "action",
		KEY_BODY_PARAMS: "params",
		KEY_BODY_ERROR: "error",
		KEY_BODY_RESULT: "result",
		VERSION: "1.0",
		ACTION_SET: "set",
		ACTION_GET: "get",

		createEvent: function(srv, action, to) {
			var evt = {
				dtalk: this.VERSION,
				service: srv,
				action: action
			};

			if (to) {
				evt.to = to;
			}

			return evt;
		},

		createEventWithParams: function(srv, action, params, to) {
			var evt = this.createEvent(srv, action, to);
			if (params) {
				evt.params = params;
			}
			return evt;
		},

		createGetEvent: function(srv, property, to) {
			return this.createEventWithParams(srv, this.ACTION_GET, property, to);
		},

		createSetEvent: function(srv, params, to) {
			return this.createEventWithParams(srv, this.ACTION_SET, params, to);
		},
		
		get: function(srv, property, callback, to, timeout) {
			DTalk.sendRequest(this.createGetEvent(srv, property, to), callback, timeout);
		},
		
		set: function(srv, params, to) {
			DTalk.sendNotification(this.createSetEvent(srv, params, to));
		},
		
		invoke: function(srv, action, params, to) {
			DTalk.sendNotification(this.createEventWithParams(srv, action, params, to));
		},
		
		invokeWithCallback: function(srv, action, params, callback, to, timeout) {
			DTalk.sendRequest(this.createEventWithParams(srv, action, params, to), callback, timeout);
		},

		subscribe: function(event, to) {
			this.invoke("dtalk.Dispatcher", "subscribe", event, to);
			
			return {
				remove: function() {
					this.invoke("dtalk.Dispatcher", "unsubscribe", event, to);
				}
			}
		},
		
		subscribeWithCallback: function(event, callback, to, timeout) {
			return DTalk.addEventListener(event, callback, true, to, timeout);
		}

	};

	return DTalkAdapter;
	
});
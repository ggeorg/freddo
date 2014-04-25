/*******************************************************************************
 * dtalk.js
 * 
 * Reference implementation of DTalk client, as specified by DTalk
 * specification.
 * 
 * Copyright 2013-2014 ArkaSoft LLC. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 . Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 ******************************************************************************/

/*******************************************************************************
 * VERSION: 0.9.2
 * 
 *  - WebPresence support added
 *  - Use always DTalk.connect(); by default this connects to the given 'ws' url
 *    parameter.
 * 
 ******************************************************************************/
(function() {
	window.DTalk = {

		/**
		 * Debug flag; set to 'false' in production environment.
		 */
		debug : true,

		_ws : null,
		_connect : function(url) {
			this._ws = new WebSocket(url || this.getURLParameter("ws"));
		},
		connect : function(url) {
			if (window.XDTalk) {
				try {
					this.getReadyState = function() {
						return 1;
					};
					this.send = function(message) {
						XDTalk.send(message);
					};
					this._subscribe = function(topic, target) {
						if (target) {
							DTalk._subscribeTo(topic, target);
						}
					};
					this._unsubscribe = function(topic, from) {
						if (from) {
							DTalk._unsubscribeFrom(topic, from);
						}
					};

					// call onopen
					setTimeout(function() {
						if (DTalk.onopen) {
							DTalk.onopen.apply(DTalk, arguments);
						}
						DTalk.publish("dtalk.onopen", {
							dtalk : "1.0",
							service : "dtalk.onopen"
						});
					}, 0);

				} catch (e) {
					console.error(e);
				}
			} else if (window.AndroidDTalk) {
				try {
					this.getReadyState = function() {
						return 1;
					};
					this.send = function(message) {
						AndroidDTalk.send(message);
					};
					this._subscribe = function(topic, target) {
						AndroidDTalk.subscribe(topic);

						if (target) {
							DTalk._subscribeTo(topic, target);
						}
					};
					this._unsubscribe = function(topic, from) {
						AndroidDTalk.unsubscribe(topic);

						if (from) {
							DTalk._unsubscribeFrom(topic, from);
						}
					};

					// call onopen
					setTimeout(function() {
						if (DTalk.onopen) {
							DTalk.onopen.apply(DTalk, arguments);
						}
						DTalk.publish("dtalk.onopen", {
							dtalk : "1.0",
							service : "dtalk.onopen"
						});
					}, 0);

				} catch (e) {
					console.error(e);
				}
			} else {
				if (!window.WebSocket) {
					window.WebSocket = window.MozWebSocket;
				}
				if (window.WebSocket) {
					try {
						this._connect(url);
						this._ws.onopen = function() {
							if (DTalk.onopen) {
								DTalk.onopen.apply(DTalk, arguments);
							}
							DTalk.publish("dtalk.onopen", {
								dtalk : "1.0",
								service : "dtalk.onopen"
							});
						};
						this._ws.onmessage = function(evt) {
							try {
								var msg = JSON.parse(evt.data);
								DTalk.publish(msg.service, msg);
							} catch (e) {
								console.error(e);
							}
						};
						this._ws.onclose = function() {
							DTalk._ws = null;

							if (DTalk.onclose) {
								DTalk.onclose.apply(DTalk, arguments);
							}
						};
						this._ws.onerror = function() {
							alert("WebSocket Error!");
						};
						this.getReadyState = function() {
							return DTalk._ws.readyState;
						};
						this.send = function(message) {
							try {
								DTalk._ws.send(message);
							} catch (e) {
								console.error(e);
							}
						};
						this._subscribe = function(topic, target) {
							var subscribe = {
								service : "dtalk.Dispatcher",
								action : "subscribe",
								params : topic
							};
							DTalk.send(JSON.stringify(subscribe));

							// subscribe also to target...
							DTalk._subscribeTo(topic, target);
						};
						this._unsubscribe = function(topic, target) {
							var unsubscribe = {
								service : "dtalk.Dispatcher",
								action : "unsubscribe",
								params : topic
							};
							DTalk.send(JSON.stringify(unsubscribe));

							// un-subscribe also from target...
							DTalk._unsubscribeFrom(topic, target);
						};
					} catch (e) {
						console.error(e);
					}
				} else {
					alert("Your browser seems to not support WebSocket !");
				}
			}
		},

		/**
		 * An event listener to be called when the WebSocket connection's
		 * readyState changes to OPEN; this indicates that the connection is
		 * ready to send and receive data. The event is a simple one with the
		 * name "open".
		 */
		onopen : null,

		/**
		 * An event listener to be called when the WebSocket connection's
		 * readyState changes to CLOSED. The listener receives a CloseEvent
		 * named "close".
		 */
		onclose : null,

		/**
		 * Return WebSocket connection state:
		 * 
		 * <pre>
		 *  0: The connection is not yet open.
		 *  1: The connection is open and ready to communicate.
		 *  2: The connection is in the process of closing.
		 *  3: The connection is closed or couldn't opened.
		 * </pre>
		 */
		getReadyState : function() {
			return 3;
		},

		/**
		 * Creates a Document Object Model (DOM) event of the specified type.
		 * <p>
		 * In the code the old fashioned way is used:
		 * https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Events/Creating_and_triggering_events
		 * <p>
		 * The reason for doing this is to support Internet Explorer as well.
		 * 
		 * @param type
		 *            A user-defined custom event type.
		 * @param data
		 *            Custom data.
		 */
		_createEvent : function(type, data) {

			// Create the event.
			var event = document.createEvent('Event');

			// Initializes a new event that the createEvent method created:
			// eventType: type, canBubble: false, cancelable: false.
			event.initEvent(type, false, false);

			if (data) {
				event['data'] = data;
			}

			return event;
		},

		/**
		 * Publish a message to a topic (used by android WebView).
		 * <p>
		 * NOTE: Android's WebView does not support the WebSocket.
		 */
		_publish : function(topic, data) {
			setTimeout(function() {
				try {
					// DTalk.publish(topic, JSON.parse(DTalk.decode(data)));
					DTalk.publish(topic, JSON.parse(data));
				} catch (e) {
					console.error(e);
				}
			}, 0);
		},

		/**
		 * Publish a JSON message, to a topic.
		 * <p>
		 * DOM event dispatching is used; this method allows the dispatch of
		 * events into the implementations event model. Events dispatched in
		 * this manner will have capturing and bubbling behavior disabled.
		 * <p>
		 * The target of the event is {@code window}.
		 */
		publish : function(topic, data) {
			try {
				// alert(topic + ' : ' + JSON.stringify(data));
				window.dispatchEvent(DTalk._createEvent(topic, data));
			} catch (e) {
				console.error(e);
			}
		},

		/** Send text message. */
		send : function(message) {
			console.error("DTalk.send() is called before DTalk.connect().");
		},

		/**
		 * A convenience method to send a notification event that is internally
		 * stringify'd
		 */
		sendNotification : function(message) {
			DTalk.send(JSON.stringify(message));
		},

		/** Send request. */
		timerH : null,
		sendRequest : function(message, callback, timeout) {
			if (!message.id) {
				message.id = DTalk.createUniqueId(message.service);
			}

			var timerH;

			var target = ('to' in message) ? message.to : null;

			var eventH = function(event) {
				try {
					if (timerH) {
						clearTimeout(timerH);
					}
					DTalk.removeEventListener(message.id, eventH, true, target);
					// callback.call(this, event.data);
					callback.call(this, event);
				} catch (e) {
					console.error(e);
				}
			};

			DTalk.addEventListener(message.id, eventH, true, target);

			timerH = setTimeout(function() {
				DTalk.removeEventListener(message.id, eventH, true, target);
				var event = DTalk._createEvent(message.id, {
					dtalk : "1.0",
					service : message.id,
					error : "timeout"
				});
				callback.call(this, event);
			}, timeout || 33333);

			DTalk.send(JSON.stringify(message));
		},

		/**
		 * Registers the specified DOM event listener.
		 * <p>
		 * If an event listener is added while it is processing an event, it
		 * will not be triggered by the current actions but may be triggered
		 * during a later stage of event flow.
		 * <p>
		 * If multiple identical event listeners are registered on the same
		 * EventTarget with the same parameters, the duplicate instances are
		 * discarded. They do not cause the EventListener to be called twice,
		 * and since the duplicates are discarded, they do not need to be
		 * removed manually with the removeEventListener method.
		 * 
		 * @param type
		 *            A string representing the event type to listen for. The a
		 *            valid XML Name:
		 *            http://www.w3.org/TR/1998/REC-xml-19980210#NT-Name
		 * 
		 * @param listener
		 *            The object that receives a notification when an event of
		 *            the specified type occurs. This must be an object
		 *            implementing the EventListener interface:
		 *            http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-EventListener,
		 *            or simply a JavaScript function.
		 * 
		 * @param register,
		 *            target (optional)
		 * 
		 * @see removeEventListener
		 */
		addEventListener : function(event, listener) {
			var register=false, target;

			try {
				window.addEventListener(event, listener, false);

				if (arguments.length >= 3 && (typeof arguments[2] === "boolean")) {
					register = arguments[2];
					if (arguments.length > 3)
						target = arguments[3];
				}

				if (register && (DTalk.getReadyState() === 1)) {
					this._subscribe(event, target);
				}

			} catch (e) {
				console.error(e);
			}
		},

		/** Remove event listener. */
		removeEventListener : function(event, listener) {
			var unregister=false, target;

			try {
				window.removeEventListener(event, listener, false);

				if (arguments.length >= 3 && (typeof arguments[2] === "boolean")) {
					unregister = arguments[2];
					if (arguments.length > 3)
						target = arguments[3];
				}

				if (unregister && (DTalk.getReadyState() === 1)) {
					this._unsubscribe(event, target);
				}

			} catch (e) {
				console.error(e);
			}
		},

		// TODO documentation
		_subscribe : function(topic, target) {
			console.error("First open a connection before sending a message !");
		},

		_subscribeTo : function(topic, target) {
			var subscribe = {
				service : "dtalk.Dispatcher",
				action : "subscribe",
				params : topic
			};

			if (target) {
				subscribe.to = target;
			}

			DTalk.send(JSON.stringify(subscribe));
		},

		// TODO documentation
		_unsubscribe : function(topic, target) {
			console.error("First open a connection before sending a message !");
		},

		_unsubscribeFrom : function(topic, target) {
			var unsubscribe = {
				service : "dtalk.Dispatcher",
				action : "unsubscribe",
				params : topic
			};

			if (target) {
				unsubscribe.to = target;
			}

			DTalk.send(JSON.stringify(unsubscribe));
		},

		/** Base64 encoding. */
		encode : function(str) {
			return window.btoa(window.unescape(window.encodeURIComponent(str)));
		},

		/** Base64 decoding. */
		decode : function(str) {
			return window.decodeURIComponent(window.escape(window.atob(str)));
		},

		/** Create unique ID. */
		_uniqueId : null,
		createUniqueId : function(prefix) {
			if (!DTalk._uniqueId) {
				DTalk._uniqueId = (new Date()).getTime();
			}
			return (prefix || 'id') + (DTalk._uniqueId++);
		},

		/** Get URL parameter: 2 different implementations to test. */
		getURLParam : function(name) {
			name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
			var regexS = "[\\?&]" + name + "=([^&#]*)";
			var regex = new RegExp(regexS);
			var results = regex.exec(window.location.href);
			if (results == null)
				return "";
			else
				return decodeURIComponent(results[1]);
		},
		getURLParameter : function(name) {
			return decodeURIComponent(((new RegExp("[?|&]" + name + "=" + "([^&;]+?)(&|#|;|$)")).exec(location.search) || [ , "" ])[1].replace(/\+/g, "%20")) || null;
		}
	};
	
	// iOS reconnect event...
	DTalk.addEventListener("freddo.websocket.reconnect", function(event) {
		alert(event.data.port);
		DTalk.connect("ws://localhost:" + parseInt(event.data.port) + "/dtalksrv");
	}, false);
	
})();
/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arkasoft.freddo.dtalk;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.netty4.client.DTalkNettyClientConnection;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkChannelClosedEvent;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

/**
 * A singleton class for message dispatching that monitors all
 * {@link IncomingMessageEvent}s and re-publishes based on topic.
 * <p>
 * Consumes service: dtalk.Dispatcher
 * </p>
 * <p>
 * Actions:
 * <ul>
 * <li>subscribe:topic</li>
 * <li>unsubscribe:topic</li>
 * </ul>
 * </p>
 * 
 * TODO: reference counting & listen to closed ws connections...
 */
public final class DTalkDispatcher {
	private static final String TAG = LOG.tag(DTalkDispatcher.class);

	private static volatile DTalkDispatcher sInstance;

	/**
	 * Start {@code DTalkDispatcher}.
	 */
	public static void start() {
		LOG.v(TAG, ">>> start");

		if (sInstance == null) {
			synchronized (DTalkDispatcher.class) {
				if (sInstance == null) {
					sInstance = new DTalkDispatcher();
				}
			}
		} else {
			LOG.w(TAG, "Allready started");
		}
	}

	// --------------------------------------------------------------------------
	// Event listeners.
	// --------------------------------------------------------------------------

	/**
	 * Listener for {@link OutgoingMessageEvent}s.
	 */
	private final MessageBusListener<OutgoingMessageEvent> mOutgoingMsgEventListener = new MessageBusListener<OutgoingMessageEvent>() {
		@Override
		public void messageSent(String topic, final OutgoingMessageEvent message) {
			LOG.v(TAG, ">>> mOutgoingMsgEventListener.messageSent: %s", topic);
//			DTalkService.getInstance().getConfiguration().getThreadPool().execute(new Runnable() {
//				@Override
//				public void run() {
					try {
						send(message);
					} catch (Throwable t) {
						LOG.e(TAG, "Unhandled exception (%s).", getClass(), t);
					}
//				}
//			});
		}
	};

	private final MessageBusListener<IncomingMessageEvent> mIncomingMsgEventListener = new MessageBusListener<IncomingMessageEvent>() {
		@Override
		public void messageSent(String topic, IncomingMessageEvent message) {
			LOG.v(TAG, ">>> mIncomingMsgEventListener.messageSent: %s", topic);
			try {
				onIncomingMessageEvent(message);
			} catch (Throwable t) {
				LOG.e(TAG, "Unhandled exception (%s).", getClass(), t);
			}
		}
	};

	// --------------------------------------------------------------------------

	/* Subscribers mapped by (sender + '@' + topic). */
	private final Map<String, Subscriber> mSubscribers;

	private DTalkDispatcher() {
		mSubscribers = new ConcurrentHashMap<String, Subscriber>();

		// Subscribe to incoming/outgoing events (Note: We never un-subscribe).
		MessageBus.subscribe(IncomingMessageEvent.class.getName(), mIncomingMsgEventListener);
		MessageBus.subscribe(OutgoingMessageEvent.class.getName(), mOutgoingMsgEventListener);
	}

	// --------------------------------------------------------------------------
	// IncomingMessageEvent handler
	// --------------------------------------------------------------------------

	/**
	 * Incoming event handler.
	 * 
	 * @param message
	 */
	protected void onIncomingMessageEvent(IncomingMessageEvent message) {
		LOG.v(TAG, ">>> onIncomingMessageEvent");

		final JSONObject jsonMsg = message.getMsg();
		final String from = message.getFrom();
		final String service = jsonMsg.optString(DTalk.KEY_BODY_SERVICE, null);

		// If 'service' is 'dtalk.Dispatcher' consume event...
		if (DTalk.SERVICE_DTALK_DISPATCHER.equals(service)) {
			if (from != null) {
				final String action = jsonMsg.optString(DTalk.KEY_BODY_ACTION, null);
				if (DTalk.ACTION_SUBSCRIBE.equals(action)) {

					//
					// Handle 'subscribe' request...
					//

					final String topic = jsonMsg.optString(DTalk.KEY_BODY_PARAMS, null);
					if (topic != null) {
						LOG.d(TAG, "Subscribe: %s (%s)", topic, from);
						final String _topic = mkTopic(from, topic);
						if (!mSubscribers.containsKey(_topic)) {
							LOG.d(TAG, "Create new subscriber for: %s", _topic);
							Subscriber subscriber = new Subscriber(from);
							mSubscribers.put(_topic, subscriber);
							MessageBus.subscribe(topic, subscriber);
						} else {
							LOG.d(TAG, "Increase subscriber's refCnt for: %s", _topic);
							mSubscribers.get(_topic).incRefCnt();
						}
					} else {
						LOG.w(TAG, "No topic to subscribe to.");
					}

				} else if (DTalk.ACTION_UNSUBSCRIBE.equals(action)) {

					//
					// Handle 'unsubscribe' request...
					//

					final String topic = jsonMsg.optString(DTalk.KEY_BODY_PARAMS, null);
					if (topic != null) {
						LOG.d(TAG, "Unsubscribe: %s (%s)", topic, from);
						final String _topic = mkTopic(from, topic);
						if (mSubscribers.containsKey(_topic)) {
							Subscriber subscriber = mSubscribers.get(_topic);
							if (subscriber != null && subscriber.decRefCnt() == 0) {
								LOG.d(TAG, "Remove subscriber: %s", _topic);
								mSubscribers.remove(_topic);
								MessageBus.unsubscribe(topic, subscriber);
							} else {
								LOG.d(TAG, "Decrease subscriber's refCnt for: %s", _topic);
							}
						}
					} else {
						LOG.w(TAG, "No topic to un-subscribe from,");
					}

				} // else: Ignore invalid event action.
			} // else: Don't handle events from unknown senders ('from' == null).

		} else if (service != null) {

			//
			// Dispatch incoming message event...
			//

			if (from != null) {
				try {
					jsonMsg.put(DTalk.KEY_FROM, from);
				} catch (JSONException e) {
					// Ignore
				}
			}

			MessageBus.sendMessage(service, jsonMsg);

		} else {
			// Should never happen!
			LOG.w(TAG, "Invalid event: %s", message);
		}
	}

	/* Makes new topic based on 'from' and 'topic'. */
	private static final String mkTopic(String from, String topic) {
		return new StringBuilder().append(from).append('@').append(topic).toString();
	}

	/**
	 * Message broadcaster/gateway used by DTalkDispatcher.
	 */
	private class Subscriber implements MessageBusListener<JSONObject> {
		private final String mRecipient;

		private volatile int mRefCnt;

		Subscriber(String recipient) {
			mRecipient = recipient;
			mRefCnt = 1;
		}

		public int incRefCnt() {
			return ++mRefCnt;
		}

		public int decRefCnt() {
			return --mRefCnt;
		}

		@Override
		public void messageSent(String topic, JSONObject message) {
			// avoid cyclic broadcast messages...
			final String from = message.optString(DTalk.KEY_FROM, null);
			if (from != null && !from.startsWith(DTalkService.LOCAL_CHANNEL_PREFIX) && from.equals(mRecipient)) {
				return;
			}

			try {
				// clone, cleanup to attribute and send it...
				JSONObject jsonMsg = new JSONObject(message.toString());
				jsonMsg.remove(DTalk.KEY_TO);
				// NOTE: don't change the 'from'
				// By keeping 'from' we do support event forwarding...
				// jsonMsg.remove(DTalk.KEY_FROM);
				MessageBus.sendMessage(new OutgoingMessageEvent(mRecipient, jsonMsg));
			} catch (JSONException e) {
				LOG.e(TAG, "JSON error: %s", e.getMessage());
			}
		}

		@Override
		public String toString() {
			return "_MessageBusListener [recipient=" + mRecipient + "]";
		}

	}

	// --------------------------------------------------------------------------
	// OutgoingMessageEvent handler
	// --------------------------------------------------------------------------

	/**
	 * Send outgoing message.
	 * 
	 * @param message
	 *          The message to send.
	 * @throws Exception
	 */
	private synchronized void send(OutgoingMessageEvent message) throws Exception {
		LOG.v(TAG, ">>> send: %s", message.toString());

		String to = message.getTo();
		if (to == null) {
			return;
		}

		// Get connection by name (recipient).
		DTalkConnection conn = DTalkConnectionRegistry.getInstance().get(to);

		if (conn != null && !conn.isOpen()) {

			// lazy clean up
			DTalkConnectionRegistry.getInstance().remove(to).close();

			// Notify listeners that channel was closed...
			MessageBus.sendMessage(new DTalkChannelClosedEvent(to));

			// force creation of a new connection...
			conn = null;
		}

		if (conn == null) {
			// Get service info or recipient by name (recipient)
			// We use direct access to the service map in service discovery instance.
			try {
			ZConfServiceInfo remoteServiceInfo = DTalkService.getInstance().getServiceInfoMap().get(to);
			if (remoteServiceInfo != null) {
				try {
					String dTalkServiceAddr = DTalkService.getWebSocketAddress(remoteServiceInfo);
					LOG.i(TAG, "Connect to: %s", dTalkServiceAddr);
					(conn = new DTalkNettyClientConnection(new URI(dTalkServiceAddr))).connect();
					// addChannel(to, ch);
					DTalkConnectionRegistry.getInstance().register(to, conn);
					// ch = getChannelByName(to);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}} catch(IllegalStateException e) {
				LOG.w(TAG, e.getMessage());
			}
		}

		if (conn != null) {
			send(conn, message);
		}
	}

	private void send(DTalkConnection conn, final OutgoingMessageEvent message) throws JSONException {
		LOG.d(TAG, ">> sending: %s", message);

		// clone message and send...
		final JSONObject jsonMsg = new JSONObject(message.getMsg().toString());
		final String service = jsonMsg.optString(DTalk.KEY_BODY_SERVICE, null);
		final String to = message.getTo();
		if (service != null) {
			if (service.startsWith("$")) {
				if (to.startsWith("x-dtalk-") && !jsonMsg.has(MessageEvent.KEY_FROM)) {
					jsonMsg.put(MessageEvent.KEY_FROM, DTalkService.getInstance().getLocalServiceInfo().getServiceName());
				}
			} else if (!jsonMsg.has(MessageEvent.KEY_FROM)) {
				jsonMsg.put(MessageEvent.KEY_FROM, DTalkService.getInstance().getLocalServiceInfo().getServiceName());
			}
		}
		jsonMsg.put(MessageEvent.KEY_TO, to);

		LOG.d(TAG, "Message: %s", jsonMsg.toString());
		Object result = conn.sendMessage(jsonMsg);
		if (result != null && result instanceof ChannelFuture) {
			((ChannelFuture) result).addListener(new GenericFutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if (!f.isSuccess()) {
						LOG.w(TAG, "Failed to send message: %s", message);
					}
				}
			});
		}
	}

}

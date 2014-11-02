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
package com.arkasoft.freddo.dtalk.j7ee.server;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.DTalkConnection;
import com.arkasoft.freddo.dtalk.DTalkConnectionRegistry;
import com.arkasoft.freddo.messagebus.MessageBus;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.util.AsyncCallback;
import freddo.dtalk.util.LOG;

@ServerEndpoint(value = "/dtalksrv", configurator = DTalkConfigurator.class)
public class DTalkServerEndpoint implements DTalkConnection {
	private static final String TAG = LOG.tag(DTalkServerEndpoint.class);

	static {
		LOG.setLogLevel(LOG.VERBOSE);
	}

	private EndpointConfig mConfig;
	private Session mSession;

	public Session getSession() {
		return mSession;
	}

	public EndpointConfig getConfig() {
		return mConfig;
	}

	public HandshakeRequest getHandshakeRequest() {
		return (HandshakeRequest) mConfig.getUserProperties().get(DTalkConfigurator.DTALK_HANDSHAKE_REQUEST_KEY);
	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		LOG.v(TAG, ">>> onOpen: %s", session.getId());

		mSession = session;
		mConfig = config;

		if (LOG.isLoggable(LOG.VERBOSE)) {
			HandshakeRequest req = getHandshakeRequest();
			LOG.v(TAG, "=================================");
			LOG.v(TAG, "QueryString   : %s", req.getQueryString());
			LOG.v(TAG, "RequestURI    : %s", req.getRequestURI());
			LOG.v(TAG, "Headers       : %s", req.getHeaders());
			LOG.v(TAG, "UserPrincipal : %s", req.getUserPrincipal());
			LOG.v(TAG, "ParameterMap  : %s", req.getParameterMap());
			HttpSession httpSession = (HttpSession) req.getHttpSession();
			if (httpSession != null) {
				Enumeration<String> e = httpSession.getAttributeNames();
				while (e.hasMoreElements()) {
					final String attr = e.nextElement();
					LOG.v(TAG, "Session[%s]: %s", attr, httpSession.getAttribute(attr));
				}
			}
			LOG.v(TAG, "=================================");
		}

		final String id = String.format("%s%s", DTalkService.LOCAL_CHANNEL_PREFIX, mSession.getId());
		DTalkConnectionRegistry.getInstance().register(id, this);

		// Notify context listener about it...
		MessageBus.sendMessage(new DTalkConnectionEvent(this, true));
	}

	@OnClose
	public void onClose() {
		LOG.v(TAG, ">>> onClose: %s", mSession.getId());

		final String id = String.format("%s%s", DTalkService.LOCAL_CHANNEL_PREFIX, mSession.getId());
		DTalkConnectionRegistry.getInstance().remove(id);

		// Notify context listener about it..
		MessageBus.sendMessage(new DTalkConnectionEvent(this, false));

		mSession = null;
	}

	@OnMessage
	public void onMessage(String message) {
		LOG.v(TAG, ">>> onMessage: %s", message);

		try {
			JSONObject jsonMsg = new JSONObject(message);

			@SuppressWarnings("unused")
			String to = jsonMsg.optString(DTalk.KEY_TO, null);
			String from = jsonMsg.optString(DTalk.KEY_FROM, null);
			String service = jsonMsg.optString(DTalk.KEY_BODY_SERVICE, null);

			// clean up message
			jsonMsg.remove(DTalk.KEY_FROM);
			jsonMsg.remove(DTalk.KEY_TO);

			JSONObject jsonBody = jsonMsg;

			if (service == null) {
				LOG.w(TAG, "Invalid Message");
				JSONObject _jsonBody = jsonBody;
				jsonBody = new JSONObject();
				jsonBody.put(DTalk.KEY_BODY_SERVICE, "dtalk.InvalidMessage");
				jsonBody.put(DTalk.KEY_BODY_PARAMS, _jsonBody);
			}

			//
			// incoming message
			//

			if (from == null) {
				// anonymous message...
				if (service != null && !service.startsWith("$")) {
					// if its not a broadcast message add 'from'...
					from = String.format("%s%s", DTalkService.LOCAL_CHANNEL_PREFIX, mSession.getId());
				}
				// else: see DTalkDispatcher for message forwarding.
			}

			LOG.d(TAG, "IncomingMessageEvent from: %s", from);
			MessageBus.sendMessage(new IncomingMessageEvent(from, jsonBody));

		} catch (Throwable t) {
			LOG.e(TAG, "Error in %s", message);
			t.printStackTrace();
		}
	}

	@OnError
	public void onError(Throwable exception, Session session) {
		LOG.e(TAG, ">>> onError: %s (%s)", exception.getMessage(), session.getId());
		exception.printStackTrace();

		try {
			session.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	void sendMessage(String msg) {
		LOG.v(TAG, ">>> sendMessage: %s", msg);
		try {
			// if (session.isOpen()) {
			mSession.getBasicRemote().sendText(msg);
			// }
		} catch (Exception e) {
			LOG.e(TAG, "%s... Closing %s", e.getMessage(), mSession.getId());
			try {
				mSession.close();
			} catch (IOException e1) {
				// Ignore
			}
		}
	}

	/**
	 * Process a received pong. This is a NO-OP.
	 * 
	 * @param pm
	 *          Ignored.
	 */
	// @OnMessage
	// public void echoPongMessage(PongMessage pm) {
	// LOG.v(TAG, ">>> echoPongMessage");
	// }

	@Override
	public String toString() {
		return "DTalkConnection [Id=" + mSession.getId() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mSession == null) ? 0 : mSession.getId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DTalkServerEndpoint other = (DTalkServerEndpoint) obj;
		if (mSession == null) {
			if (other.mSession != null)
				return false;
		} else if (!mSession.getId().equals(other.mSession.getId()))
			return false;
		return true;
	}

	// -----------------------------------------------------------------------
	// DTalkConnection implementation
	// -----------------------------------------------------------------------

	@Override
	public Object getId() {
		return mSession.getId();
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendMessage(JSONObject message, AsyncCallback<Boolean> callback) throws JSONException {
		try {
			sendMessage(message.toString());
			callback.onSuccess(Boolean.TRUE);
		} catch (Throwable caught) {
			callback.onFailure(caught);
		}
	}

	@Override
	public void onMessage(JSONObject message) throws JSONException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		if (isOpen()) {
			try {
				mSession.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean isOpen() {
		return mSession != null ? mSession.isOpen() : false;
	}

}

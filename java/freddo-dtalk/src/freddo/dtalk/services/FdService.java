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
package freddo.dtalk.services;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

/**
 * Services must extend this class.
 */
public abstract class FdService implements MessageBusListener<JSONObject> {
	private static final String TAG = LOG.tag(FdService.class);

	private final DTalkServiceContext mContext;

	protected final String mName;
	protected final String mReplyName;

	private boolean mDisposed = false;

	protected FdService(DTalkServiceContext context, String name) {
		this.mContext = context;
		this.mName = name;
		this.mReplyName = '$' + name;

		MessageBus.subscribe(getName(), this);
	}

	protected DTalkServiceContext getContext() {
		return mContext;
	}

	protected String getName() {
		return mName;
	}

	/**
	 * The final call you receive before your service is disposed.
	 * <p>
	 * Note: except of {@link FdServiceMgr}'s {@code #onDispose()} method,
	 * {@code #onDispose()} for any other service will run in UI thread.
	 * 
	 * @see #dispose()
	 */
	protected abstract void onDisposed();

	/**
	 * Check if service was disposed.
	 * 
	 * @return {@code true} if service is desposed; {@code false} otherwise.
	 */
	protected final boolean isDisposed() {
		return mDisposed;
	}

	/**
	 * Dispose this service.
	 * <p>
	 * Note: this method is called by {@link FdServiceMgr#dispose()}.
	 * <p>
	 * Note: except of {@link FdServiceMgr}'s {@code #dispose()} method,
	 * {@code #dispose()} for any other service will run in UI thread.
	 */
	public final void dispose() {
		LOG.v(LOG.tag(getClass()), ">>> dispose");

		if (!isDisposed()) {
			mDisposed = true;

			if (MessageBus.hasListener(getName(), this)) {
				MessageBus.unsubscribe(getName(), this);
			}
			
			onDisposed();
		}
	}

	// -----------------------------------------------------------------------
	// MESSAGE HANDLER API
	// -----------------------------------------------------------------------

	/**
	 * Incoming message handler which synchronizes incoming message handling with
	 * the UI thread.
	 * 
	 * @param topic
	 * @param message
	 */
	@Override
	public final void messageSent(final String topic, final JSONObject message) {
		LOG.v(getName(), ">>> messageSent");

		if (isDisposed()) {
			LOG.e(getName(), "Service disposed!!!");
			return;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!onMessage(topic, message)) {
					LOG.d(TAG, "Unhandled message: %s", topic);
				}
			}
		});
	}

	/**
	 * Utility method for synchronizing execution with the UI thread.
	 * 
	 * @see DTalkServiceContext#runOnUiThread(Runnable)
	 */
	protected final void runOnUiThread(Runnable r) {
		getContext().runOnUiThread(r);
	}

	/**
	 * Incoming message handler.
	 * <p>
	 * Note: this handler is executed in the UI thread.
	 * 
	 * @param topic
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean onMessage(String topic, JSONObject message) {
		if (isDisposed()) {
			LOG.e(getName(), "Service disposed!!!");
			return false;
		}

		final String action = message.optString(DTalk.KEY_BODY_ACTION, null);
		if (DTalk.ACTION_GET.equals(action)) {
			String property = message.optString(MessageEvent.KEY_BODY_PARAMS, null);
			if (property != null && property.length() > 0) {
				invoke("get" + cap1stChar(property), message);
				return true;
			}
		} else if (DTalk.ACTION_SET.equals(action)) {

			// TODO need to test it

			JSONObject options = message.optJSONObject(DTalk.KEY_BODY_PARAMS);
			if (options != null) {
				// NOTE: we avoid ConcurrentModificationException
				String[] keys = new String[options.length()];
				int i = 0;
				for (Iterator<String> it = options.keys(); it.hasNext();) {
					keys[i++] = it.next();
				}
				for (String key : keys) {
					invoke("set" + cap1stChar(key), options);
				}
				return true;
			}

		} else if (action != null && action.length() > 0) {
			invoke("do" + cap1stChar(action), message);
			return true;
		}
		return false;
	}

	/*
	 * Utility method to invoke a Java method by name.
	 */
	private void invoke(String method, JSONObject message) {
		LOG.v(getName(), ">>> invoke: %s::%s", getClass().getName(), method);

		try {
			Method m = getClass().getMethod(method, JSONObject.class);
			m.invoke(this, message);
		} catch (Exception e) {
			LOG.e(getName(), e.getMessage(), e);
		}
	}

	/*
	 * Utility method to capitalize the first character of a string.
	 */
	private static String cap1stChar(String str) {
		String result = str;
		if (str.length() > 0) {
			String first = str.substring(0, 1);
			result = str.replaceFirst(first, first.toUpperCase());
		}
		return result;
	}

	// --------------------------------------------------------------------------
	// RESPONSE & ERROR RESPONSE API
	// --------------------------------------------------------------------------

	private static final JSONObject newResponse(JSONObject request) throws JSONException {
		final String id = request.optString(DTalk.KEY_BODY_ID, null);
		if (id != null) {
			final JSONObject response = new JSONObject();
			final String from = request.optString(DTalk.KEY_FROM, null);
			if (from != null) {
				response.put(MessageEvent.KEY_TO, from);
			}
			response.put(MessageEvent.KEY_BODY_VERSION, "1.0");
			response.put(MessageEvent.KEY_BODY_SERVICE, id);
			return response;
		} else {
			return null;
		}
	}

	private static final void sendResponse(JSONObject jsonMsg) throws JSONException {
		if (!jsonMsg.isNull(DTalk.KEY_TO)) {
			String recipient = jsonMsg.optString(DTalk.KEY_TO);
			LOG.d(TAG, "Send response to recipient: %s", recipient);
			jsonMsg.remove(DTalk.KEY_FROM);
			jsonMsg.remove(DTalk.KEY_TO);
			MessageBus.sendMessage(new OutgoingMessageEvent(recipient, jsonMsg));
		} else {
			String service = jsonMsg.optString(DTalk.KEY_BODY_SERVICE);
			LOG.d(TAG, "Send response to: %s", service);
			if (service != null) {
				MessageBus.sendMessage(service, jsonMsg);
			}
		}
	}

	protected static void sendResponse(JSONObject request, Object value) {
		try {
			JSONObject response = newResponse(request);
			if (response != null) {
				response.put(MessageEvent.KEY_BDOY_RESULT, value);
				sendResponse(response);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void sendErrorResponse(JSONObject request, Object error) {
		try {
			JSONObject response = newResponse(request);
			if (response != null) {
				response.put(MessageEvent.KEY_BODY_ERROR, error);
				sendResponse(response);
			}
		} catch (JSONException e) {
			LOG.e(TAG, e.getMessage());
		}
	}

	protected static void sendErrorResponse(JSONObject request, int code, String message) {
		sendErrorResponse(request, code, message, null);
	}

	protected static void sendErrorResponse(JSONObject request, int code, String message, JSONObject data) {
		JSONObject error = new JSONObject();
		try {
			error.put(DTalk.KEY_ERROR_CODE, code);
			error.put(DTalk.KEY_ERROR_MESSAGE, message);
			if (data != null) {
				error.put(DTalk.KEY_ERROR_DATA, data);
			}
			sendErrorResponse(request, error);
		} catch (JSONException e) {
			LOG.e(TAG, e.getMessage());
		}
	}

	protected static void sendErrorResponse(JSONObject request, DTalkException e) {
		sendErrorResponse(request, e.toJSON());
	}

	// -----------------------------------------------------------------------
	// EVENT BROADCASTING API
	// -----------------------------------------------------------------------

	/**
	 * Create new broadcast event.
	 * 
	 * @param event
	 *          the event name.
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject newEvent(String event) throws JSONException {
		JSONObject message = new JSONObject();
		message.put(MessageEvent.KEY_BODY_VERSION, "1.0");
		message.put(MessageEvent.KEY_BODY_SERVICE, mReplyName + "." + event);
		return message;
	}

	/**
	 * Broadcast event to all subscribers.
	 * 
	 * @param event
	 *          the event name.
	 */
	protected void fireEvent(String event) {
		try {
			JSONObject message = newEvent(event);
			MessageBus.sendMessage(message.optString(MessageEvent.KEY_BODY_SERVICE), message);
		} catch (JSONException e) {
			LOG.e(getName(), e.getMessage());
		}
	}

	/**
	 * Broadcast event to all subscribers.
	 * 
	 * @param event
	 *          the event name.
	 * @param params
	 *          event parameters
	 */
	protected void fireEvent(String event, Object params) {
		try {
			JSONObject message = newEvent(event);
			message.put(MessageEvent.KEY_BODY_PARAMS, params);
			MessageBus.sendMessage(message.optString(MessageEvent.KEY_BODY_SERVICE), message);
		} catch (JSONException e) {
			LOG.e(getName(), e.getMessage());
		}
	}

	// --------------------------------------------------------------------------

	@Deprecated
	protected JSONObject getJSONObject(JSONObject options, String key) {
		JSONObject object = !options.isNull(key) ? options.optJSONObject(key) : null;
		options.remove(key);
		return object;
	}

	@Deprecated
	protected JSONArray getJSONArray(JSONObject options, String key) {
		JSONArray array = !options.isNull(key) ? options.optJSONArray(key) : null;
		options.remove(key);
		return array;
	}

	@Deprecated
	protected int getInt(JSONObject options, String key) {
		int value = options.optInt(key);
		options.remove(key);
		return value;
	}

	@Deprecated
	protected double getDouble(JSONObject options, String key) {
		double value = options.optDouble(key);
		options.remove(key);
		return value;
	}

	@Deprecated
	protected boolean getBoolean(JSONObject options, String key) {
		boolean value = options.optBoolean(key);
		options.remove(key);
		return value;
	}

	@Deprecated
	protected String getString(JSONObject options, String key) {
		String value = !options.isNull(key) ? options.optString(key) : null;
		options.remove(key);
		return value;
	}

}

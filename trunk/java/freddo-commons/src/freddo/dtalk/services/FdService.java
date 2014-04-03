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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

public abstract class FdService<T> implements MessageBusListener<JSONObject> {
  private static final String TAG = LOG.tag(FdService.class);

  protected static final String SRV_PREFIX = "dtalk.service.";

  private final T context;
  private final String name;
  protected final String replyName;

  private boolean disposed = false;

  final Map<String, String> refCntMap = new ConcurrentHashMap<String, String>();

  protected FdService(T context, String name, final JSONObject options) {
    this.context = context;
    this.name = name;
    this.replyName = '$' + name;

    MessageBus.subscribe(getName(), this);
  }

  protected abstract void start();

  protected abstract void reset();

  protected T getContext() {
    return context;
  }

  protected String getName() {
    return name;
  }

  @Override
  public void messageSent(final String topic, final JSONObject message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!onMessage(topic, message)) {
          LOG.d(TAG, "Unhandled message: %s", topic);
        }
      }
    });
  }

  protected abstract void runOnUiThread(Runnable r);

  @SuppressWarnings("unchecked")
  private boolean onMessage(String topic, JSONObject message) {
    String action = message.optString(DTalk.KEY_BODY_ACTION);
    if ("get".equals(action)) {
      String property = message.optString(MessageEvent.KEY_BODY_PARAMS, null);
      if (property != null && property.length() > 0) {
        invoke("get" + cap1stChar(property), message);
        return true;
      }
    } else if ("set".equals(action)) {
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

  private void invoke(String method, JSONObject message) {
    LOG.v(TAG, ">>> invoke: %s::%s", getClass().getName(), method);

    try {
      Method m = getClass().getMethod(method, JSONObject.class);
      m.invoke(this, message);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // --------------------------------------------------------------------------
  // RESPONSE
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

  protected static void sendErrorResponse(JSONObject request, Object error) {
    try {
      JSONObject response = newResponse(request);
      if (response != null) {
        response.put(MessageEvent.KEY_BODY_ERROR, error);
        sendResponse(response);
      }
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // --------------------------------------------------------------------------

  protected JSONObject getJSONObject(JSONObject options, String key) {
    JSONObject object = !options.isNull(key) ? options.optJSONObject(key) : null;
    options.remove(key);
    return object;
  }

  protected JSONArray getJSONArray(JSONObject options, String key) {
    JSONArray array = !options.isNull(key) ? options.optJSONArray(key) : null;
    options.remove(key);
    return array;
  }

  protected int getInt(JSONObject options, String key) {
    int value = options.optInt(key);
    options.remove(key);
    return value;
  }

  protected double getDouble(JSONObject options, String key) {
    double value = options.optDouble(key);
    options.remove(key);
    return value;
  }

  protected boolean getBoolean(JSONObject options, String key) {
    boolean value = options.optBoolean(key);
    options.remove(key);
    return value;
  }

  protected String getString(JSONObject options, String key) {
    String value = !options.isNull(key) ? options.optString(key) : null;
    options.remove(key);
    return value;
  }

  private JSONObject newEvent(String event) throws JSONException {
    JSONObject message = new JSONObject();
    message.put(MessageEvent.KEY_BODY_VERSION, "1.0");
    message.put(MessageEvent.KEY_BODY_SERVICE, replyName + "." + event);
    return message;
  }

  protected void fireEvent(String event) {
    try {
      JSONObject message = newEvent(event);
      MessageBus.sendMessage(message.optString(MessageEvent.KEY_BODY_SERVICE), message);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  protected void fireEvent(String event, Object value) {
    try {
      JSONObject message = newEvent(event);
      message.put(MessageEvent.KEY_BODY_PARAMS, value);
      MessageBus.sendMessage(message.optString(MessageEvent.KEY_BODY_SERVICE), message);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  protected final boolean isDisposed() {
    return disposed;
  }

  public final void dispose() {
    LOG.v(LOG.tag(getClass()), ">>> dispose");

    disposed = true;
    reset();

    MessageBus.unsubscribe(getName(), this);
  }

  private static String cap1stChar(String str) {
    String result = str;
    if (str.length() > 0) {
      String first = str.substring(0, 1);
      result = str.replaceFirst(first, first.toUpperCase());
    }
    return result;
  }
}

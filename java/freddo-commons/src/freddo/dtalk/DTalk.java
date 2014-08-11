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
package freddo.dtalk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.util.LOG;

public final class DTalk implements MessageEvent {
  private static final String TAG = LOG.tag(DTalk.class);

  public static final String SERVICE_TYPE = "_http._tcp.local.";

  // --------------------------------------------------------------------------

  public static final String SERVICE_INVALID_MESSAGE = "dtalk.InvalidMessage";
  public static final String SERVICE_INVALID_RESULT = "dtalk.InvalidResult";

  public static final String SERVICE_DTALK_PRESENCE = "dtalk.Presence";

  public static final String ACTION_RESOLVED = "resolved";
  public static final String ACTION_REMOVED = "removed";

  private static final String ACTION_GET = "get";
  private static final String ACTION_SET = "set";

  public static final String ACTION_START = "start";
  public static final String ACTION_STOP = "stop";

  // --------------------------------------------------------------------------

  public static final String KEY_NAME = "name";
  public static final String KEY_SERVER = "server";
  public static final String KEY_PORT = "port";

  public static final String KEY_PRESENCE_DTALK = "dtalk";
  public static final String KEY_PRESENCE_DTYPE = "dtype";

  public static final String SERVICE_DTALK_DISPATCHER = "dtalk.Dispatcher";

  public static final String ACTION_SUBSCRIBE = "subscribe";
  public static final String ACTION_UNSUBSCRIBE = "unsubscribe";

  // --------------------------------------------------------------------------

  public final static long DEFAULT_TIMEOUT = 33333L;

  // --------------------------------------------------------------------------

  private static ScheduledExecutorService sScheduledExecutorService = null;

  private static ScheduledExecutorService ensureScheduledExecutorServiceExists() {
    if (sScheduledExecutorService == null) {
      sScheduledExecutorService = Executors.newScheduledThreadPool(5);
    }
    return sScheduledExecutorService;
  }

  private static int sCount = 0;

  public static synchronized String createUniqueId(String prefix) {
    return String.format("%s-%d", prefix == null ? KEY_BODY_VERSION : prefix, sCount++);
  }

  // --------------------------------------------------------------------------
  // COMMON DTALK MESSAGING
  // --------------------------------------------------------------------------

  public static JSONObject createMessage(String target, String service) throws JSONException {
    JSONObject message = new JSONObject();
    message.put(KEY_TO, target);
    message.put(KEY_BODY_VERSION, "1.0");
    message.put(KEY_BODY_SERVICE, service);
    return message;
  }

  public static JSONObject createMessage(String target, String service, String action) throws JSONException {
    JSONObject message = createMessage(target, service);
    message.put(KEY_BODY_ACTION, action);
    return message;
  }

  public static JSONObject createMessage(String target, String service, String action, Object params) throws JSONException {
    JSONObject message = createMessage(target, service, action);
    message.put(KEY_BODY_PARAMS, params);
    return message;
  }

  //
  // INVOKE
  //

  public static void invoke(String target, String service, String action) throws DTalkException {
    try {
      DTalk.send(createMessage(target, service, action));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void invoke(String target, String service, String action, boolean param) throws DTalkException {
    try {
      DTalk.send(createMessage(target, service, action, param));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void invoke(String target, String service, String action, double param) throws DTalkException {
    try {
      DTalk.send(createMessage(target, service, action, param));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void invoke(String target, String service, String action, int param) throws DTalkException {
    try {
      DTalk.send(createMessage(target, service, action, param));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void invoke(String target, String service, String action, Object param) throws DTalkException {
    try {
      DTalk.send(createMessage(target, service, action, param));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  //
  // INVOKE with AsyncCallback<T>
  //

  public static <T> void invoke(String target, String service, String action, AsyncCallback<T> callback) {
    invoke(target, service, action, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void invoke(String target, String service, String action, AsyncCallback<T> callback, long timeout) {
    try {
      DTalk.send(createMessage(target, service, action), callback, timeout);
    } catch (Exception e) {
      processInvocationException(e, callback);
    }
  }

  public static <T> void invoke(String target, String service, String action, boolean params, AsyncCallback<T> callback) {
    invoke(target, service, action, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void invoke(String target, String service, String action, boolean params, AsyncCallback<T> callback, long timeout) {
    try {
      DTalk.send(createMessage(target, service, action, params), callback, timeout);
    } catch (Exception e) {
      processInvocationException(e, callback);
    }
  }

  public static <T> void invoke(String target, String service, String action, double params, AsyncCallback<T> callback) {
    invoke(target, service, action, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void invoke(String target, String service, String action, double params, AsyncCallback<T> callback, long timeout) {
    try {
      DTalk.send(createMessage(target, service, action, params), callback, timeout);
    } catch (Exception e) {
      processInvocationException(e, callback);
    }
  }

  public static <T> void invoke(String target, String service, String action, int params, AsyncCallback<T> callback) {
    invoke(target, service, action, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void invoke(String target, String service, String action, int params, AsyncCallback<T> callback, long timeout) {
    try {
      DTalk.send(createMessage(target, service, action, params), callback, timeout);
    } catch (Exception e) {
      processInvocationException(e, callback);
    }
  }

  public static <T> void invoke(String target, String service, String action, Object params, AsyncCallback<T> callback) {
    invoke(target, service, action, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void invoke(String target, String service, String action, Object params, AsyncCallback<T> callback, long timeout) {
    try {
      DTalk.send(createMessage(target, service, action, params), callback, timeout);
    } catch (Exception e) {
      processInvocationException(e, callback);
    }
  }

  private static <T> void processInvocationException(Exception e, AsyncCallback<T> callback) {
    if (callback != null) {
      callback.onFailure(e);
    } else {
      LOG.e(TAG, "Invocation error: %s", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  //
  // GET
  //

  public static <T> void sendGet(String target, String service, String property, AsyncCallback<T> callback) {
    DTalk.invoke(target, service, ACTION_GET, property, callback, DEFAULT_TIMEOUT);
  }

  public static <T> void sendGet(String target, String service, String property, AsyncCallback<T> callback, long timeout) {
    DTalk.invoke(target, service, ACTION_GET, property, callback, timeout);
  }

  //
  // SET
  //

  public static void sendSet(String target, String service, String property, boolean value) throws DTalkException {
    try {
      DTalk.invoke(target, service, ACTION_SET, new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void sendSet(String target, String service, String property, double value) throws DTalkException {
    try {
      DTalk.invoke(target, service, ACTION_SET, new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void sendSet(String target, String service, String property, int value) throws DTalkException {
    try {
      DTalk.invoke(target, service, ACTION_SET, new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void sendSet(String target, String service, String property, Object value) throws DTalkException {
    try {
      DTalk.invoke(target, service, ACTION_SET, new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public static void sendSet(String target, String service, JSONObject options) throws DTalkException {
    DTalk.invoke(target, service, ACTION_SET, options);
  }

  // --------------------------------------------------------------------------
  // LOW LEVEL MESSAGING
  // --------------------------------------------------------------------------

  private final static ExecutorService sThreadPool = Executors.newCachedThreadPool();

  /**
   * Forward message, don't do any processing.
   * 
   * @param service
   * @param message
   * @throws DTalkException
   */
  public static void forward(final String service, final JSONObject message) throws DTalkException {
    if (service != null && service.trim().length() > 0) {
      sThreadPool.execute(new Runnable() {
        @Override
        public void run() {
          MessageBus.sendMessage(service, message);
        }
      });
    } else {
      throw new DTalkException(DTalkException.INTERNAL_ERROR, "Invalid service or null");
    }
  }

  /**
   * Send notification.
   * 
   * @param notification
   * @throws DTalkException
   */
  public static void send(JSONObject notification) throws DTalkException {
    if (!(notification.has(KEY_BODY_SERVICE) && notification.has(KEY_BODY_ACTION))) {
      throw new DTalkException(DTalkException.INTERNAL_ERROR, "Invalid message");
    }
    forward(notification.optString(KEY_BODY_SERVICE, null), notification);
  }

  /**
   * Send request with default timeout.
   * 
   * @param request
   * @param callback
   * @throws DTalkException
   */
  public static <T> void send(JSONObject request, final AsyncCallback<T> callback) throws DTalkException {
    send(request, callback, DEFAULT_TIMEOUT);
  }

  /**
   * Send request with timeout.
   * 
   * @param request
   * @param callback
   * @param delay
   * @throws DTalkException
   */
  public static <T> void send(JSONObject request, final AsyncCallback<T> callback, long delay) throws DTalkException {

    // if request callback is null, then send just a notification.
    if (callback == null) {
      send(request);
      return;
    }

    // validate DTalk message
    if (!(request.has(KEY_BODY_SERVICE) && request.has(KEY_BODY_ACTION))) {
      throw new DTalkException(DTalkException.INTERNAL_ERROR, "Invalid message");
    }

    try {

      final String requestId = mkRequestId(request);
      if (requestId == null) {
        throw new DTalkException(DTalkException.INTERNAL_ERROR, "Invalid message");
      }

      // create callback handler
      final MessageBusListener<JSONObject> mListener = new MessageBusListener<JSONObject>() {
        @SuppressWarnings("unchecked")
        @Override
        public void messageSent(String topic, JSONObject message) {
          if (MessageBus.hasListener(requestId, this)) {
            try {
              MessageBus.unsubscribe(requestId, this);
              if (message.has(KEY_BODY_ERROR)) {
                callback.onFailure(new RuntimeException(message.optString(KEY_BODY_ERROR)));
              } else if (message.has(KEY_BDOY_RESULT)) {
                callback.onSuccess((T) message.opt(KEY_BDOY_RESULT));
              } else {
                callback.onFailure(new RuntimeException(SERVICE_INVALID_RESULT));
              }
            } catch (Throwable t) {
              LOG.e(TAG, "Handler error");
              t.printStackTrace();
            }
          }
        }
      };

      // subscribe to requestId
      MessageBus.subscribe(requestId, mListener);

      try {
        forward(request.optString(KEY_BODY_SERVICE, null), request);
      } catch (DTalkException e) {
        try {
          MessageBus.unsubscribe(requestId, mListener);
          callback.onFailure(e);
        } catch (Throwable t) {
          LOG.e(TAG, "Handler error");
          t.printStackTrace();
        }
        return;
      }

      // setup timeout timer
      ensureScheduledExecutorServiceExists().schedule(new Runnable() {
        @Override
        public void run() {
          if (MessageBus.hasListener(requestId, mListener)) {
            try {
              MessageBus.unsubscribe(requestId, mListener);
              callback.onFailure(new DTalkException(DTalkException.REQUEST_TIMEOUT, "Timeout"));
            } catch (Throwable t) {
              LOG.e(TAG, "Timeout handler error");
              t.printStackTrace();
            }
          }
        }
      }, delay, TimeUnit.MILLISECONDS);

    } catch (JSONException e1) {
      throw new DTalkException(DTalkException.INVALID_JSON, e1.getMessage());
    }
  }

  /**
   * Subscribe to topic.
   * 
   * @param topic
   * @param listener
   * @return
   */
  public static DTalkSubscribeHandle subscribe(final String topic, final DTalkEventListener listener) {
    // do subscribe
    MessageBus.subscribe(topic, listener);

    // create and return un-subscribe handle
    return new DTalkSubscribeHandle() {
      @Override
      public void remove() {
        try {
          MessageBus.unsubscribe(topic, listener);
        } catch (Exception e) {
          LOG.e(TAG, e.getMessage());
        }
      }
    };
  }

  /** Utility method to inject 'id' in request if missing. */
  private static String mkRequestId(JSONObject request) throws JSONException {
    // check if 'id' is missing, if yes create one...
    String requestId = request.has(KEY_BODY_ID) && !request.isNull(KEY_BODY_ID) ? request.optString(KEY_BODY_ID) : null;
    if (requestId == null || requestId.trim().length() == 0) {
      String service = request.optString(KEY_BODY_SERVICE);
      request.put(KEY_BODY_ID, requestId = createUniqueId(service));
    }
    return requestId;
  }

  private DTalk() {
  }
}

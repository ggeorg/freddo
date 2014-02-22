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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.MessageEvent;

public final class DTalk implements MessageEvent {
  public static final String SERVICE_TYPE = "_http._tcp.local.";

  public static final String SERVICE_INVALID_MESSAGE = "dtalk.InvalidMessage";
  public static final String SERVICE_INVALID_RESULT = "dtalk.InvalidResult";

  public static final String SERVICE_DTALK_PRESENCE = "dtalk.Presence";

  public static final String ACTION_RESOLVED = "resolved";
  public static final String ACTION_REMOVED = "removed";

  public static final String KEY_NAME = "name";
  public static final String KEY_SERVER = "server";
  public static final String KEY_PORT = "port";
  
  public static final String KEY_PRESENCE_DTALK = "dtalk";
  public static final String KEY_PRESENCE_DTYPE = "dtype";

  public static final String SERVICE_DTALK_DISPATCHER = "dtalk.Dispatcher";

  public static final String ACTION_SUBSCRIBE = "subscribe";
  public static final String ACTION_UNSUBSCRIBE = "unsubscribe";

  // --------------------------------------------------------------------------

  private final static ScheduledExecutorService sScheduledExecutorService = Executors.newScheduledThreadPool(5);

  private static int count = 0;

  public static synchronized String createUniqueId(String prefix) {
    return String.format("%s-%d", prefix == null ? MessageEvent.KEY_BODY_VERSION : prefix, count++);
  }

  public static void send(JSONObject message) {
    final String service = message.optString(MessageEvent.KEY_BODY_SERVICE, null);
    if (service != null && service.trim().length() > 0) {
      MessageBus.sendMessage(service, message);
    }
  }

  public static <T> void send(JSONObject message, final AsyncCallback<T> callback) throws JSONException {
    send(message, callback, 33333L);
  }

  public static <T> void send(JSONObject message, final AsyncCallback<T> callback, long delay) throws JSONException {
    assert (message.has(MessageEvent.KEY_BODY_SERVICE) && message.has(MessageEvent.KEY_BODY_ACTION)) : "Invalid message";
    assert callback != null : "AsyncCallback is null";

    final String requestId = mkRequestId(message);
    final MessageBusListener<JSONObject> l = new MessageBusListener<JSONObject>() {
      @SuppressWarnings("unchecked")
      @Override
      public void messageSent(String topic, JSONObject message) {
        MessageBus.unsubscribe(requestId, this);
        if (message.has(DTalk.KEY_BODY_ERROR)) {
          callback.onFailure(new RuntimeException(message.optString(DTalk.KEY_BODY_ERROR)));
        } else if (message.has(DTalk.KEY_BDOY_RESULT)) {
          callback.onSuccess((T) message.opt(DTalk.KEY_BDOY_RESULT));
        } else {
          callback.onFailure(new RuntimeException(DTalk.SERVICE_INVALID_RESULT));
        }
      }
    };
    MessageBus.subscribe(requestId, l);
    sScheduledExecutorService.schedule(new Runnable() {
      @Override
      public void run() {
        MessageBus.unsubscribe(requestId, l);

        // TODO send timeout
      }
    }, delay, TimeUnit.MILLISECONDS);

    send(message);
  }

  public static DTalkSubscribeHandle subscribe(final String topic, final DTalkEventListener listener) {
    MessageBus.subscribe(topic, listener);
    return new DTalkSubscribeHandle() {
      @Override
      public void remove() {
        MessageBus.unsubscribe(topic, listener);
      }
    };
  }

  private static String mkRequestId(JSONObject message) throws JSONException {
    // check if 'id' is missing, if yes create one...
    String requestId = message.has(MessageEvent.KEY_BODY_ID)
        && !message.isNull(MessageEvent.KEY_BODY_ID) ? message.optString(MessageEvent.KEY_BODY_ID) : null;
    if (requestId == null || requestId.trim().length() == 0) {
      String service = message.optString(MessageEvent.KEY_BODY_SERVICE);
      message.put(MessageEvent.KEY_BODY_ID, requestId = createUniqueId(service));
    }
    return requestId;
  }

  private DTalk() {
  }
}

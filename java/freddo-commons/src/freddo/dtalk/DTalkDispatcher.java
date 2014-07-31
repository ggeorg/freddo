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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

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
        if (sInstance == null)
          sInstance = new DTalkDispatcher();
      }
    } else {
      LOG.w(TAG, "Allready started");
    }
  }

  private final Map<String, _MessageBusListener> mSubscribers;
  
  private final MessageBusListener<IncomingMessageEvent> mIncomingMessageEventHandler = new MessageBusListener<IncomingMessageEvent>() {
    @Override
    public void messageSent(String topic, IncomingMessageEvent message) {
      try {
        onIncomingMessageEvent(message);
      } catch (Throwable t) {
        LOG.e(TAG, "Unhandled exception:", t);
      }
    }
  };

  private DTalkDispatcher() {
    mSubscribers = new ConcurrentHashMap<String, _MessageBusListener>();

    // Subscribe to incoming events (Note: We never un-subscribe).
    MessageBus.xsubscribe(IncomingMessageEvent.class.getName(), mIncomingMessageEventHandler);
  }

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

    // If 'service' is 'dtalk.Dispatcher' consume the event...
    if (DTalk.SERVICE_DTALK_DISPATCHER.equals(service)) {
      if (from != null) {
        final String action = jsonMsg.optString(DTalk.KEY_BODY_ACTION, null);
        if (DTalk.ACTION_SUBSCRIBE.equals(action)) {
          // Handle 'subscribe' request...
          final String topic = jsonMsg.optString(DTalk.KEY_BODY_PARAMS, null);
          if (topic != null) {
            LOG.d(TAG, "subscribe: %s (%s)", topic, from);
            if (!mSubscribers.containsKey(from + topic)) {
              _MessageBusListener subscriber = new _MessageBusListener(from);
              mSubscribers.put(from + topic, subscriber);
              MessageBus.xsubscribe(topic, subscriber);
            } // XXX: reference counting?
          } // else: No topic to subscribe to, ignore event.
        } else if (DTalk.ACTION_UNSUBSCRIBE.equals(action)) {
          final String topic = jsonMsg.optString(DTalk.KEY_BODY_PARAMS, null);
          if (topic != null) {
            LOG.d(TAG, "unsubscribe: %s", topic);
            if (mSubscribers.containsKey(from + topic)) {
              _MessageBusListener subscriber = mSubscribers.remove(from + topic);
              if (subscriber != null) {
                MessageBus.unsubscribe(topic, subscriber);
              }
            } // XXX: reference counting?
          } // else: No topic to un-subscribe from, ignore event.
        } // else: Ignore invalid event action.
      } // else: Don't handle events from unknown senders ('from' == null).
    } else if (service != null) {
      // Dispatch message event...
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

  /**
   * Message broadcaster/gateway used by DTalkDispatcher.
   */
  private class _MessageBusListener implements MessageBusListener<JSONObject> {
    private final String recipient;

    _MessageBusListener(String recipient) {
      this.recipient = recipient;
    }

    @Override
    public void messageSent(String topic, JSONObject message) {
      // avoid cyclic broadcast messages...
      final String from = message.optString(DTalk.KEY_FROM, null);
      if (from != null && !from.startsWith(DTalkService.LOCAL_CHANNEL_PREFIX) && from.equals(recipient)) {
        return;
      }

      try {
        // clone, cleanup to attribute and send it...
        JSONObject jsonMsg = new JSONObject(message.toString());
        jsonMsg.remove(DTalk.KEY_TO);
        // NOTE: don't change the 'from'
        // By keeping 'from' we do support event forwarding...
        // jsonMsg.remove(DTalk.KEY_FROM);
        MessageBus.sendMessage(new OutgoingMessageEvent(recipient, jsonMsg));
      } catch (JSONException e) {
        LOG.e(TAG, "JSON error: %s", e.getMessage());
      }
    }

    @Override
    public String toString() {
      return "_MessageBusListener [recipient=" + recipient + "]";
    }

  }
}

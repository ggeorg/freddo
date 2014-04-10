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
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

/**
 * A singleton class for message dispatching that monitors all
 * {@link IncomingMessageEvent}s and republishes based on topic.
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
 */
public final class DTalkDispatcher {
  private static final String TAG = LOG.tag(DTalkDispatcher.class);

  private static volatile DTalkDispatcher instance;

  public static void start() {
    LOG.v(TAG, ">>> start");

    if (instance == null) {
      instance = new DTalkDispatcher();
    } else {
      LOG.w(TAG, "Allready started");
    }
  }

  private final Map<String, _MessageBusListener> mSubscribers;

  private DTalkDispatcher() {
    mSubscribers = new ConcurrentHashMap<String, _MessageBusListener>();
    MessageBus.subscribe(IncomingMessageEvent.class.getName(), new MessageBusListener<IncomingMessageEvent>() {
      @Override
      public void messageSent(String topic, IncomingMessageEvent message) {
        onIncomingMessageEvent(message);
      }
    });
  }

  protected void onIncomingMessageEvent(IncomingMessageEvent pMessage) {
    LOG.v(TAG, ">>> onIncomingMessageEvent");

    final JSONObject jsonMsg = pMessage.getMsg();
    final String from = pMessage.getFrom();
    final String service = jsonMsg.optString(MessageEvent.KEY_BODY_SERVICE, null);

    if (DTalk.SERVICE_DTALK_DISPATCHER.equals(service)) {
      if (from != null) {
        String action = jsonMsg.optString(MessageEvent.KEY_BODY_ACTION, null);
        if (DTalk.ACTION_SUBSCRIBE.equals(action)) {
          String topic = jsonMsg.optString(MessageEvent.KEY_BODY_PARAMS, null);
          if (topic != null) {
            LOG.d(TAG, "subscribe: %s (%s)", topic, from);
            if (!mSubscribers.containsKey(from + topic)) {
              _MessageBusListener subscriber = new _MessageBusListener(from);
              mSubscribers.put(from + topic, subscriber);
              MessageBus.subscribe(topic, subscriber);
            }
          }
        } else if (DTalk.ACTION_UNSUBSCRIBE.equals(action)) {
          String topic = jsonMsg.optString(MessageEvent.KEY_BODY_PARAMS, null);
          if (topic != null) {
            LOG.d(TAG, "unsubscribe: %s", topic);
            if (mSubscribers.containsKey(from + topic)) {
              _MessageBusListener subscriber = mSubscribers.remove(from + topic);
              if (subscriber != null) {
                MessageBus.unsubscribe(topic, subscriber);
              }
            }
          }
        } else {
          // Ignore invalid action
        }
      } else {
        // Ignore from == null
      }
    } else if (service != null) {

      // Dispatch message event...
      if (from != null) {
        try {
          jsonMsg.put(MessageEvent.KEY_FROM, from);
        } catch (JSONException e) {
          // Ignore
        }
      }

      // Publish message with topic
      MessageBus.sendMessage(service, jsonMsg);
    }
  }

  private class _MessageBusListener implements MessageBusListener<JSONObject> {
    private final String recipient;

    _MessageBusListener(String recipient) {
      this.recipient = recipient;
    }

    @Override
    public void messageSent(String topic, JSONObject message) {
      try {
        // clone, cleanup to attribute and send it...
        JSONObject jsonMsg = new JSONObject(message.toString());
        jsonMsg.remove(MessageEvent.KEY_TO);
        // NOTE: don't change the 'from'
        // By keeping 'from' we do support event forwarding...
        // jsonMsg.remove(MessageEvent.KEY_FROM);
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

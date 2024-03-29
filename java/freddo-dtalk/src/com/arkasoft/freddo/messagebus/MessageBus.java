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
package com.arkasoft.freddo.messagebus;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Provides support for basic intra-application message passing.
 * <p>
 * Keep in mind that message bus events are handled synchronously. So, you want
 * to take case that any events handled in this fashion are handled quickly.
 * Otherwise, your application's performance could be negatively impacted.
 */
public class MessageBus {
  private static final Map<String, ListenerList<MessageBusListener<?>>> sMessageTopics =
      new WeakHashMap<String, ListenerList<MessageBusListener<?>>>();

  /**
   * Subscribes a listener to a message topic.
   * 
   * @param topic
   * @param messageListener
   */
  public static <T> void subscribe(String topic, MessageBusListener<T> messageListener) {
    synchronized (sMessageTopics) {
      ListenerList<MessageBusListener<?>> topicListeners = sMessageTopics.get(topic);

      if (topicListeners == null) {
        topicListeners = new ListenerList<MessageBusListener<?>>();
        sMessageTopics.put(topic, topicListeners);
      }

      topicListeners.add(messageListener);
    }
  }

  /**
   * Unsubscribes a listener from a message topic.
   * 
   * @param topic
   * @param messageListener
   */
  public static <T> void unsubscribe(String topic, MessageBusListener<T> messageListener) {
    synchronized (sMessageTopics) {
      ListenerList<MessageBusListener<?>> topicListeners = sMessageTopics.get(topic);

      if (topicListeners == null) {
        throw new IllegalArgumentException(topic + " does not exist.");
      }

      topicListeners.remove(messageListener);
      if (topicListeners.isEmpty()) {
        sMessageTopics.remove(topic);
      }
    }
  }

  /**
   * Tests the existence of a listener for a topic.
   * 
   * @param topic
   * @param messageListener
   * @return
   */
  public static <T> boolean hasListener(String topic, MessageBusListener<T> messageListener) {
    synchronized (sMessageTopics) {
      ListenerList<MessageBusListener<?>> topicListeners = sMessageTopics.get(topic);

      if (topicListeners != null) {
        return topicListeners.contains(messageListener);
      }

      return false;
    }
  }

  /**
   * Sends a message to subscribed topic listeners.
   * 
   * @param topic
   * @param message
   */
  @SuppressWarnings("unchecked")
  public static <T> void sendMessage(String topic, T message) {
    synchronized (sMessageTopics) {
      ListenerList<MessageBusListener<?>> topicListeners = sMessageTopics.get(topic);

      if (topicListeners != null) {
        for (MessageBusListener<?> listener : topicListeners) {
          try {
            ((MessageBusListener<T>) listener).messageSent(topic, message);
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Sends a message to subscribed topic listeners.
   * 
   * @param message
   */
  public static <T> void sendMessage(T message) {
    sendMessage(message.getClass().getName(), message);
  }

  /**
   * Sends an asynchronous message to subscribed topic listeners by using the
   * provided {@code ExecutorService}.
   * 
   * @param topic
   * @param message
   * @param threadPool
   */
  public static <T> void sendMessage(final String topic, final T message, ExecutorService threadPool) {
    threadPool.execute(new Runnable() {
      @Override
      public void run() {
        MessageBus.sendMessage(topic, message);
      }
    });
  }

  /**
   * Sends an asynchronous message to subscribed topic listeners by using the
   * provided {@code ExecutorService}.
   * 
   * @param message
   * @param threadPool
   */
  public static <T> void sendMessage(final T message, ExecutorService threadPool) {
    threadPool.execute(new Runnable() {
      @Override
      public void run() {
        sendMessage(message.getClass().getName(), message);
      }
    });
  }

}

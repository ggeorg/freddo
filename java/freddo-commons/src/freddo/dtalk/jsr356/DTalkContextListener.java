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
package freddo.dtalk.jsr356;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkDispatcher;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

public abstract class DTalkContextListener implements ServletContextListener, DTalkServiceContext {
  private static final String TAG = LOG.tag(DTalkContextListener.class);

  private final Map<String, DTalkConnection> mConnections = new ConcurrentHashMap<String, DTalkConnection>();

  public DTalkContextListener() {
    // Start dispatcher...
    try {
      DTalkDispatcher.start();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /** DTalkConnectionEvent listener. */
  private final MessageBusListener<DTalkConnectionEvent> dtalkConnectionEL = new MessageBusListener<DTalkConnectionEvent>() {
    @Override
    public void messageSent(String topic, DTalkConnectionEvent message) {
      DTalkConnection conn = message.getConnection();
      if (message.isOpen()) {
        addConnection(conn);
      } else {
        removeConnection(conn);
      }
    }
  };

  /** IncomingMessageEvent listener. */
  private final MessageBusListener<IncomingMessageEvent> incomingEventListener = new MessageBusListener<IncomingMessageEvent>() {
    @Override
    public void messageSent(String topic, IncomingMessageEvent message) {
      try {
        dispatchMessage(message);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  };

  /** OutgoingMessageEvent listener. */
  private final MessageBusListener<OutgoingMessageEvent> outgoingEventListener = new MessageBusListener<OutgoingMessageEvent>() {
    @Override
    public void messageSent(String topic, OutgoingMessageEvent message) {
      try {
        sendMessage(message);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  };

  protected void addConnection(DTalkConnection conn) {
    LOG.v(TAG, ">>> addConnection: %s", conn);
    mConnections.put(conn.getSession().getId(), conn);
  }

  protected void removeConnection(DTalkConnection conn) {
    LOG.v(TAG, ">>> removeConnection: %s", conn);
    mConnections.remove(conn.getSession().getId());
  }

  protected void resetConnections() {
    LOG.v(TAG, ">>> resetConnection");
    
    mConnections.clear();
  }

  public DTalkConnection getConnection(String id) {
    LOG.v(TAG, ">>> getConnection: %s", id);

    // remove prefix (we added in DTalkConnection).
    if (id.startsWith(DTalkService.LOCAL_CHANNEL_PREFIX)) {
      id = id.substring(DTalkService.LOCAL_CHANNEL_PREFIX.length());
    }

    return mConnections.get(id);
  }

  protected void sendMessage(OutgoingMessageEvent message) throws Exception {
    LOG.v(TAG, ">>> sendMessage: %s", message);
    String to = message.getTo();
    JSONObject jsonMsg = message.getMsg();
    sendMessage(to, jsonMsg.toString());
  }

  protected void sendMessage(String to, String message) {
    LOG.v(TAG, ">>> sendMessage to: %s, message: %s", to, message);
    if (to != null) { // TODO message validation
      DTalkConnection conn = getConnection(to);
      if (conn != null) {
        conn.sendMessage(message);
      } else {
        LOG.w(TAG, "Connection %s not found!", to);
      }

    } else {
      // TODO broadcast?
    }
  }

  protected void dispatchMessage(IncomingMessageEvent message) throws Exception {
    JSONObject jsonMsg = message.getMsg();
    String service = jsonMsg.optString(MessageEvent.KEY_BODY_SERVICE);
    String action = jsonMsg.optString(MessageEvent.KEY_BODY_ACTION);
    if (service != null && action != null) {
      MessageBus.sendMessage(service, jsonMsg);
    }
  }

  protected abstract void stopServices();

  // {
  // synchronized (serviceList) {
  // for (Iterator<Service> iter = serviceList.iterator(); iter.hasNext();) {
  // Service c = iter.next();
  // try {
  // c.stop();
  // } catch (Exception e) {
  // // Ignore
  // } finally {
  // iter.remove();
  // }
  // }
  // }
  // }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    LOG.v(TAG, ">>> contextInitialized");

    //
    // TODO read configuration settings
    //

    resetConnections();

    MessageBus.xsubscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
    MessageBus.xsubscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
    MessageBus.xsubscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    LOG.v(TAG, ">>> contextDestroyed");

    try {
      stopServices();
    } finally {
      MessageBus.unsubscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
      MessageBus.unsubscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
      MessageBus.unsubscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
    }

    resetConnections();
  }
}

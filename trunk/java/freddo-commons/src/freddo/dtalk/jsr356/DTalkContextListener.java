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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

public abstract class DTalkContextListener implements ServletContextListener {
  private static final String TAG = LOG.tag(DTalkContextListener.class);

  private final Map<String, DTalkConnection> connections = new ConcurrentHashMap<String, DTalkConnection>();

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
    connections.put(conn.getSession().getId(), conn);
  }

  protected void removeConnection(DTalkConnection conn) {
    LOG.v(TAG, ">>> removeConnection: %s", conn);
    connections.remove(conn.getSession().getId());
  }

  protected void resetConnections() {
    LOG.v(TAG, ">>> resetConnection");
    connections.clear();
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
      DTalkConnection conn = connections.get(to);
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

  private List<Service> serviceList = new ArrayList<Service>();

  protected final void addService(String topic, Service service) {
    synchronized (serviceList) {
      MessageBus.subscribe(topic, service);
      serviceList.add(service);
      service.start();
    }
  }

  private void stopServices() {
    synchronized (serviceList) {
      for (Iterator<Service> iter = serviceList.iterator(); iter.hasNext();) {
        Service c = iter.next();
        try {
          c.stop();
        } catch (Exception e) {
          // Ignore
        } finally {
          iter.remove();
        }
      }
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    LOG.v(TAG, "contextInitialized");
    
    resetConnections();
    
    MessageBus.subscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
    MessageBus.subscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
    MessageBus.subscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    LOG.v(TAG, "contextDestroyed");
    
    stopServices();
    
    MessageBus.unsubscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
    MessageBus.unsubscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
    MessageBus.unsubscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
    
    resetConnections();
  }
}
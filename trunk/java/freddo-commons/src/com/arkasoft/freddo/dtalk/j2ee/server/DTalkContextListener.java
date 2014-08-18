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
package com.arkasoft.freddo.dtalk.j2ee.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.DTalkDispatcher;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;

public abstract class DTalkContextListener implements ServletContextListener, DTalkServiceContext {
  private static final String TAG = LOG.tag(DTalkContextListener.class);

  //private final Map<String, DTalkConnectionImpl> mConnections = new ConcurrentHashMap<String, DTalkConnectionImpl>();

  /** DTalkConnectionEvent listener. */
  private final MessageBusListener<DTalkConnectionEvent> dtalkConnectionEL = new MessageBusListener<DTalkConnectionEvent>() {
    @Override
    public void messageSent(String topic, DTalkConnectionEvent message) {
      DTalkServerEndpoint conn = message.getConnection();
      if (message.isOpen()) {
        onConnectionOpen(conn);
      } else {
        onConnectionClose(conn);
      }
    }
  };

  protected void onConnectionOpen(DTalkServerEndpoint conn) {
  	// do nothing here
  }

  protected void onConnectionClose(DTalkServerEndpoint conn) {
  	// do nothing here
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
    
 // Start DTalkService...
    try {
      DTalkDispatcher.start();
    } catch (Throwable t) {
      t.printStackTrace();
    }

//    resetConnections();
//
//    MessageBus.subscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
//    MessageBus.subscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
//    MessageBus.subscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    LOG.v(TAG, ">>> contextDestroyed");

//    try {
//      stopServices();
//    } finally {
//      MessageBus.unsubscribe(IncomingMessageEvent.class.getName(), incomingEventListener);
//      MessageBus.unsubscribe(OutgoingMessageEvent.class.getName(), outgoingEventListener);
//      MessageBus.unsubscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
//    }
//
//    resetConnections();
  }
}

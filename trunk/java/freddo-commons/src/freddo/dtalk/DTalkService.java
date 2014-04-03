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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.netty4.client.WebSocketClient;
import com.arkasoft.freddo.dtalk.netty4.server.WebPresenceService;
import com.arkasoft.freddo.dtalk.netty4.server.WebSocketServer;
import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.DTalkChannelClosedEvent;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.events.WebPresenceEvent;
import freddo.dtalk.util.LOG;

public class DTalkService {
  private static final String TAG = LOG.tag(DTalkService.class);

  public static interface Configuration {
    JmDNS getJmDNS();

    String getDeviceId();

    String getType();

    String getTargetName();

    ExecutorService getThreadPool();

    boolean isWebPresence();

    String getWebPresenceURL();

    int getPort();
  }

  private static volatile DTalkService sInstance = null;

  public static DTalkService getInstance() {
    assert sInstance != null : "DTalkService not initialized";
    return sInstance;
  }

  public static DTalkService init(Configuration config) {
    LOG.v(TAG, ">>> init");
    assert sInstance == null : "DTalkService already initialized";
    return sInstance = new DTalkService(config);
  }

  private final Configuration config;

  private WebSocketServer webSocketServer;
  private static final Object sDTalkServiceLock = new Object();

  private ServiceInfo localServiceInfo = null;

  private DTalkDiscovery serviceDiscovery;

  private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

  private final MessageBusListener<WebPresenceEvent> webPresenceEventListener = new MessageBusListener<WebPresenceEvent>() {
    @Override
    public void messageSent(String topic, WebPresenceEvent message) {
      if (!message.isOpen()) {
        onWebPresenceClosed();
      }
    }
  };

  private DTalkService(Configuration config) {
    this.config = config;

    // Start dispatcher...
    DTalkDispatcher.start();
  }

  public Configuration getConfig() {
    return config;
  }

  public InetSocketAddress getWebSocketServerAddress() {
    return webSocketServer.getAddress();
  }

  public ServiceInfo getLocalServiceInfo() {
    return localServiceInfo;
  }

  public Channel getChannelByName(String serviceName) {
    Channel ch = channels.get(serviceName);
    if (ch != null) {
      if (!ch.isOpen()) {
        channels.remove(serviceName);
        ch.close();
        ch = null;
      }
    }
    return ch;
  }

  private static String hashCode(Channel ch) {
    return String.valueOf(ch.hashCode());
  }

  public void addChannel(Channel ch) {
    LOG.v(TAG, ">>> addChannel");

    addChannel(hashCode(ch), ch);
  }

  public void addChannel(String serviceName, Channel ch) {
    LOG.v(TAG, ">>> addChannel: %s", serviceName);

    if (!channels.containsKey(serviceName)) {
      channels.remove(hashCode(ch));
      channels.put(serviceName, ch);
    }
  }

  public void removeChannel(Channel ch) {
    LOG.v(TAG, ">>> removeChannel");

    // first try to remove channel by hash code
    final String hashCode = hashCode(ch);
    if (channels.containsKey(hashCode)) {
      removeChannel(hashCode);
      return;
    }

    // then try by instance
    String key = null;
    for (Map.Entry<String, Channel> entry : channels.entrySet()) {
      if (ch == entry.getValue()) {
        key = entry.getKey();
        break;
      }
    }
    removeChannel(key);
  }

  public void removeChannel(String key) {
    if (key != null) {
      channels.remove(key);
      MessageBus.sendMessage(new DTalkChannelClosedEvent(key));
    }
  }

  private final MessageBusListener<OutgoingMessageEvent> outgoingMsgEventListener = new MessageBusListener<OutgoingMessageEvent>() {
    @Override
    public void messageSent(String topic, final OutgoingMessageEvent message) {
      config.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            send(message);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      });
    }
  };

  private boolean mStarted =  false;

  public boolean isStarted() {
    return mStarted;
  }
  
  protected void setStarted(boolean started) {
    mStarted = started;
  }

  public String getLocalDTalkServiceAddress() {
    return getDTalkServiceAddress(getLocalServiceInfo());
  }

  public String getDTalkServiceAddress(ServiceInfo info) {
    StringBuilder sb = new StringBuilder();
    sb.append("ws://");
    sb.append(getAddress(info)).append(':').append(info.getPort());
    sb.append(WebSocketServer.WEBSOCKET_PATH);
    return sb.toString();
  }

  /**
   * Send outgoing message.
   * 
   * @param message The message to send.
   * @throws Exception
   */
  private synchronized void send(OutgoingMessageEvent message) throws Exception {
    LOG.v(TAG, ">>> send: %s", message.toString());

    String to = message.getTo();
    if (to == null) {
      return;
    }

    // Get channel by name (recipient).
    Channel ch = getChannelByName(to);

    if (ch != null && !ch.isOpen()) {
      // lazy clean up
      removeChannel(to);
      ch = null;
    }

    if (ch == null) {
      // Get service info or recipient by name (recipient)
      ServiceInfo remoteInfo = serviceDiscovery.getServiceInfoMap().get(to);
      if (remoteInfo != null) {
        try {
          String dTalkServiceAddr = getDTalkServiceAddress(remoteInfo);
          LOG.i(TAG, "Connect to: %s", dTalkServiceAddr);
          ch = new WebSocketClient(new URI(dTalkServiceAddr)).connect();
          addChannel(to, ch);
          ch = getChannelByName(to);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    if (ch != null) {
      send(ch, message);
    }
  }

  private void send(Channel ch, final OutgoingMessageEvent message) throws JSONException {
    LOG.d(TAG, ">> sending: %s (%s)", ch.isActive(), message);

    // clone message and send...
    JSONObject jsonMsg = new JSONObject(message.getMsg().toString());
    if (!jsonMsg.has(MessageEvent.KEY_FROM)) {
      jsonMsg.put(MessageEvent.KEY_FROM, getLocalServiceInfo().getName());
    }
    jsonMsg.put(MessageEvent.KEY_TO, message.getTo());

    LOG.d(TAG, "Message: %s", jsonMsg.toString());
    ch.writeAndFlush(new TextWebSocketFrame(jsonMsg.toString())).addListener(new GenericFutureListener<ChannelFuture>() {
      @Override
      public void operationComplete(ChannelFuture f) throws Exception {
        if (!f.isSuccess()) {
          LOG.w(TAG, "Failed to send message: %s", message);
        }
      }
    });
  }

  public void startup() {
    LOG.v(TAG, ">>> startup");

    synchronized (sDTalkServiceLock) {
      if (isStarted() ) {
        return;
      }
      
      setStarted(true);
      
      if (webSocketServer == null) {
        webSocketServer = new WebSocketServer(this);
      }

      if (serviceDiscovery != null) {
        serviceDiscovery.shutdown();
        serviceDiscovery = null;
      }
      serviceDiscovery = new DTalkDiscovery(this);

      LOG.d(TAG, "Subscribe for: %s", OutgoingMessageEvent.class.getName());
      MessageBus.subscribe(OutgoingMessageEvent.class.getName(), outgoingMsgEventListener);

      LOG.d(TAG, "Subscribe for: %s", WebPresenceEvent.class.getName());
      MessageBus.subscribe(WebPresenceEvent.class.getName(), webPresenceEventListener);

      config.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            // this will block execution until server is down...
            webSocketServer.startup(new Runnable() {
              @Override
              public void run() {
                publishService();

                // start the service discovery...
                if (serviceDiscovery != null) {
                  serviceDiscovery.startup();
                }
              }
            });

            // server is down...
            unpublishService();
          } catch (Exception e) {
            LOG.e(TAG, "Exception in WebSocket server startup: ", e);
          }
        }
      });
    }
  }

  public void shutdown() throws Exception {
    LOG.v(TAG, ">>> shutdown");

    synchronized (sDTalkServiceLock) {
      if (!isStarted()) {
        return;
      }
      
      try {
        LOG.d(TAG, "Unsubscribe from: %s", OutgoingMessageEvent.class.getName());
        MessageBus.unsubscribe(OutgoingMessageEvent.class.getName(), outgoingMsgEventListener);
      } catch (Exception e) {
        // ignore
      }

      try {
        LOG.d(TAG, "Unsubscribe from: %s", WebPresenceEvent.class.getName());
        MessageBus.unsubscribe(WebPresenceEvent.class.getName(), webPresenceEventListener);
      } catch (Exception e) {
        // ignore
      }

      if (serviceDiscovery != null) {
        serviceDiscovery.shutdown();
        serviceDiscovery = null;
      }

      // closing client connections...
      for (Map.Entry<String, Channel> entry : channels.entrySet()) {
        Channel ch = entry.getValue();
        if (ch != null && ch.isOpen()) {
          LOG.d(TAG, "Closing connection to: %s", entry.getKey());
          try {
            ch.close();
          } catch (Exception e) {
            // Ignore
          }
        }
      }

      LOG.d(TAG, "Clean up connections...");
      channels.clear();

      // stop WebSocket server...
      if (webSocketServer != null) {
        unpublishService();
        webSocketServer.shutdown();
      }
      
      setStarted(false);
    }
  }

  /**
   * NOTE: This event will not get fired after a call to {@link #shutdown()}.
   */
  protected void onWebPresenceClosed() {
    config.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(3333L);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        // synchronized (sDTalkServiceLock) {
        enableWebPresence(config.isWebPresence());
        // }
      }
    });
  }

  public void publishService() {
    LOG.v(TAG, ">>> publishService");

    String targetName = config.getTargetName();
    if (targetName == null || targetName.trim().length() == 0) {
      return;
    }

    String deviceId = config.getDeviceId();
    if (deviceId != null && deviceId.trim().length() > 0) {
      targetName += "@" + deviceId;
    }

    // register service on the allocated port
    Map<String, String> props = new HashMap<String, String>();
    props.put(DTalk.KEY_PRESENCE_DTALK, "1");
    props.put(DTalk.KEY_PRESENCE_DTYPE, config.getType());
    // ...

    try {
      setServiceInfo(ServiceInfo.create(DTalk.SERVICE_TYPE, targetName, webSocketServer.getAddress().getPort(), 0, 0, props));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void unpublishService() {
    LOG.v(TAG, ">>> unpublishService");

    try {
      setServiceInfo(null);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void republishService() {
    try {
      unpublishService();
    } finally {
      publishService();
    }
  }

  public void enableWebPresence(boolean webPresence) {
    LOG.v(TAG, ">>> enableWebPresence: %b", webPresence);

    if (webPresence) {
      try {
        String webPresenceURI = config.getWebPresenceURL();
        if (webPresenceURI == null || webPresenceURI.trim().length() == 0) {
          return;
        }
        getWebPresenceService().publish(new URI(webPresenceURI), config.getJmDNS(), localServiceInfo);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        getWebPresenceService().unpublish();
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  public ServiceInfo getServiceInfo(String name) {
    return getServiceInfoMap().get(name);
  }

  public Map<String, ServiceInfo> getServiceInfoMap() {
    return serviceDiscovery != null ? serviceDiscovery.getServiceInfoMap() : new HashMap<String, ServiceInfo>();
  }

  private WebPresenceService webPresenceService = null;

  protected WebPresenceService getWebPresenceService() {
    if (webPresenceService == null) {
      webPresenceService = new WebPresenceService();
    }
    return webPresenceService;
  }

  private synchronized void setServiceInfo(ServiceInfo serviceInfo) throws IOException {
    if (this.localServiceInfo != null) {

      // Notify that DTalkService is about to unregister.
      MessageBus.sendMessage(new DTalkServiceEvent());

      // Unregister service.
      config.getJmDNS().unregisterService(localServiceInfo);

      // WebPresence...
      enableWebPresence(false);
    }

    localServiceInfo = serviceInfo;

    if (this.localServiceInfo != null) {

      // Notify that DTalkService is ready to handle local connections.
      MessageBus.sendMessage(new DTalkServiceEvent(localServiceInfo));

      // Publish service.
      config.getJmDNS().registerService(localServiceInfo);

      // WebPresence...
      enableWebPresence(config.isWebPresence());
    }
  }

  public static final String getAddress(ServiceInfo info) {
    // NOTE: info.getServer() takes to long to resolve the IP address.
    String server = null; // info.getServer();
    // if (server == null || server.trim().length() == 0) {
    InetAddress[] addresses = info.getInetAddresses();
    if (addresses.length > 0) {
      for (InetAddress address : addresses) {
        server = address.getHostAddress();
        break;
      }
    } else {
      // fallback to info.getServer()
      server = info.getServer();
      if (server == null || server.trim().length() == 0) {
        LOG.w(TAG, "Can't get address from %s", info);
      }
    }
    // }
    return server;
  }
}

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
package freddo.dtalk.server;

import java.io.IOException;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.util.LOG;

import freddo.dtalk.events.IncomingMessageEvent;

@ServerEndpoint(value = "/dtalksrv", configurator = DTalkConfigurator.class)
public class DTalkConnection {
  private static final String TAG = LOG.tag(DTalkConnection.class);

  static {
    LOG.setLogLevel(LOG.VERBOSE);
  }

  private Session session;

  private HandshakeRequest request;

  public Session getSession() {
    return session;
  }

  public HandshakeRequest getHandshakeRequest() {
    return request;
  }

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) {
    request = (HandshakeRequest) config.getUserProperties().get("handshake-req");
    // Map<String, List<String>> headers = req.getHeaders();
    //
    // LOG.e(TAG, "====================" + req.getParameterMap());
    // LOG.e(TAG, "==================x=" + headers);

    this.session = session;

    MessageBus.sendMessage(new DTalkConnectionEvent(this, true));
    // DTalkContextListener.addConnection(session.getId(), this);
  }

  @OnClose
  public void onClose() {
    MessageBus.sendMessage(new DTalkConnectionEvent(this, false));

    // DTalkContextListener.removeConnection(session.getId());
    this.session = null;
  }

  @OnMessage
  public void onMessage(String message) {
    try {
      JSONObject jsonMsg = new JSONObject(message);
      // String body = jsonMsg.optString(MessageEvent.KEY_BODY);
      // if (body != null) {
      JSONObject jsonBody = jsonMsg; // new JSONObject(body);
      // TODO jsonMsg validation
      MessageBus.sendMessage(new IncomingMessageEvent(session.getId(), jsonBody));
      // }
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @OnError
  public void onError(Throwable exception, Session session) {
    LOG.i(TAG, "Error for client: %s", session.getId());

    try {
      session.close();
    } catch (IOException e) {
      // Ignore
    }
  }

  void sendMessage(String msg) {
    LOG.v(TAG, ">>> sendMessage: %s", msg);
    
    try {
      if (session.isOpen()) {
        session.getBasicRemote().sendText(msg);
      }
    } catch (IOException ioe) {
      try {
        session.close();
      } catch (IOException e1) {
        // Ignore
      }
    }
  }

  /**
   * Process a received pong. This is a NO-OP.
   * 
   * @param pm Ignored.
   */
  @OnMessage
  public void echoPongMessage(PongMessage pm) {
    // NO-OP
  }

  @Override
  public String toString() {
    return "DTalkConnection [Id=" + session.getId() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((session == null) ? 0 : session.getId().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DTalkConnection other = (DTalkConnection) obj;
    if (session == null) {
      if (other.session != null)
        return false;
    } else if (!session.getId().equals(other.session.getId()))
      return false;
    return true;
  }

}

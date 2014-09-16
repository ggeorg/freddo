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
package com.arkasoft.freddo.dtalk.j7ee.server;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * An implementation of {@link javax.websocket.server.ServerEndpointConfig} for
 * use in Freddo applications.
 * <p>
 * This configurator stores the {@link javax.websocket.server.HandshakeRequest}
 * object into the user properties for later use.
 */
public class DTalkConfigurator extends ServerEndpointConfig.Configurator {
  public static final String DTALK_HANDSHAKE_REQUEST_KEY = "dtalk-handshake-req";

  @Override
  public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse resp) {
    super.modifyHandshake(conf, req, resp);
    conf.getUserProperties().put(DTALK_HANDSHAKE_REQUEST_KEY, req);
  }

}

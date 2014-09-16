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
package freddo.dtalk.jsr356.services;

import javax.websocket.server.HandshakeRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.j7ee.server.DTalkContextListener;
import com.arkasoft.freddo.dtalk.j7ee.server.DTalkServerEndpoint;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;

@Deprecated
public abstract class FdJSR356Service extends FdService {

  protected FdJSR356Service(DTalkServiceContext context, String name, JSONObject options) {
    super(context, name, options);
  }
  
//  public HandshakeRequest getHandshakeRequest(JSONObject message) throws JSONException {
//    String from = message.getString(DTalk.KEY_FROM);
//    if (from != null) {
//      DTalkContextListener ctx = (DTalkContextListener) getContext();
//      DTalkConnectionImpl conn = ctx.getConnection(from);
//      if (conn != null) {
//        return conn.getHandshakeRequest();
//      }
//    }
//    return null;
//  }

}

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
package freddo.dtalk.events;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;

public class OutgoingMessageEvent {

  private final JSONObject msg;

  public OutgoingMessageEvent(JSONObject msg) throws JSONException {
    this.msg = msg;
  }

  public OutgoingMessageEvent(String to, JSONObject msg) throws JSONException {
    this.msg = msg;
    this.msg.put(DTalk.KEY_TO, to);
  }

  public String getTo() {
    return msg.optString(DTalk.KEY_TO, null);
  }

  public JSONObject getMsg() {
    return msg;
  }

  @Override
  public String toString() {
    return "OutgoingMessageEvent [msg=" + msg + "]";
  }

}

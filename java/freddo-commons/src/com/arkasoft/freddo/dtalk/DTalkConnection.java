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
package com.arkasoft.freddo.dtalk;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.util.AsyncCallback;

public interface DTalkConnection {
  
  Object getId();

  void connect();

  void sendMessage(JSONObject message, AsyncCallback<Boolean> callback) throws JSONException;

  // XXX not used so far
  // could be a common message handler for all types of connections
  @Deprecated
  void onMessage(JSONObject message) throws JSONException;

  void close();
  
  boolean isOpen();

}

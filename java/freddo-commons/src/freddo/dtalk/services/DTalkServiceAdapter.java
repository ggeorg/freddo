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
package freddo.dtalk.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalk;

public class DTalkServiceAdapter {

  public static JSONObject newEvent(String service, String action) throws JSONException {
    JSONObject sEventObj = new JSONObject();
    sEventObj.put(DTalk.KEY_BODY_VERSION, "1.0");
    sEventObj.put(DTalk.KEY_BODY_SERVICE, service);
    sEventObj.put(DTalk.KEY_BODY_ACTION, action);
    return sEventObj;
  }

  public static void sendStart(Service service) throws JSONException { // synchronous
    JSONObject event = newEvent("dtalk.Services", "start");
    event.put(DTalk.KEY_BODY_PARAMS, service.getName());
    DTalk.send(event);
  }

  public static void sendStop(Service service) throws JSONException { // synchronous
    JSONObject event = newEvent("dtalk.Services", "stop");
    event.put(DTalk.KEY_BODY_PARAMS, service.getName());
    DTalk.send(event);
  }
  
  //
  // Get
  //

  public static <T> void get(Service service, String property, final AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), "get");
    event.put(DTalk.KEY_BODY_PARAMS, property);
    DTalk.send(event, callback);
  }
  
  //
  // Set
  //
  
  public static void sendSet(Service service, JSONObject options) throws JSONException {
    JSONObject event = newEvent(service.getName(), "set");
    event.put(DTalk.KEY_BODY_PARAMS, options);
    DTalk.send(event);
  }
  
  public static void set(Service service, String property, String newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }

  public static void set(Service service, String property, boolean newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }

  public static void set(Service service, String property, int newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }

  public static void set(Service service, String property, double newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }

  public static void set(Service service, String property, JSONObject newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }

  public static void set(Service service, String property, JSONArray newValue) throws JSONException {
    JSONObject options = new JSONObject();
    options.put(property, newValue);
    sendSet(service, options);
  }
  public static void set(Service service,  JSONObject options) throws JSONException {
    sendSet(service, options);
  }
  
  //
  // Invoke
  //
  
  public static void invoke(Service service, String action) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, String params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, boolean params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, int params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, double params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, JSONObject params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  public static void invoke(Service service, String action, JSONArray params) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event);
  }
  
  //
  // Invoke width callback
  //
  
  public static <T> void invoke(Service service, String action, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, String params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, boolean params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, int params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, double params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, JSONObject params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
  
  public static <T> void invoke(Service service, String action, JSONArray params, AsyncCallback<T> callback) throws JSONException {
    JSONObject event = newEvent(service.getName(), action);
    event.put(DTalk.KEY_BODY_PARAMS, params);
    DTalk.send(event, callback);
  }
 
}

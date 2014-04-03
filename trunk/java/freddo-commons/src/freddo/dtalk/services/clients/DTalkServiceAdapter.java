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
package freddo.dtalk.services.clients;

import org.json.JSONObject;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;

public class DTalkServiceAdapter {
  
  //
  // DTalk service manager start/stop
  //
  
  private static final String DTALK_SERVICES = "dtalk.Services";

  public static <T> void sendStart(Service service, final AsyncCallback<T> callback, long timeout) {
    DTalk.invoke(service.getTarget(), DTALK_SERVICES, DTalk.ACTION_START, service.getName(), callback, timeout);
  }

  public static void sendStop(Service service) throws DTalkException  {
    DTalk.invoke(service.getTarget(), DTALK_SERVICES, DTalk.ACTION_STOP, service.getName());
  }

  //
  // Get
  //
  
  public static <T> void get(Service service, String property, final AsyncCallback<T> callback) throws DTalkException {
    DTalk.sendGet(service.getTarget(), service.getName(), property, callback, DTalk.DEFAULT_TIMEOUT);
  }

  public static <T> void get(Service service, String property, final AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.sendGet(service.getTarget(), service.getName(), property, callback, timeout);
  }

  //
  // Set
  //

  public static void set(Service service, String property, boolean value) throws DTalkException {
    DTalk.sendSet(service.getTarget(), service.getName(), property, value);
  }
  
  public static void set(Service service, String property, double value) throws DTalkException {
    DTalk.sendSet(service.getTarget(), service.getName(), property, value);
  }

  public static void set(Service service, String property, int value) throws DTalkException  {
    DTalk.sendSet(service.getTarget(), service.getName(), property, value);
  }

  public static void set(Service service, String property, Object value) throws DTalkException  {
    DTalk.sendSet(service.getTarget(), service.getName(), property, value);
  }

  public static void set(Service service, JSONObject options) throws DTalkException  {
    DTalk.sendSet(service.getTarget(), service.getName(), options);
  }

  //
  // Invoke
  //

  public static void invoke(Service service, String action) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action);
  }

  public static void invoke(Service service, String action, boolean params) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params);
  }

  public static void invoke(Service service, String action, double params) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params);
  }
  
  public static void invoke(Service service, String action, int params) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params);
  }
  
  public static void invoke(Service service, String action, Object params) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params);
  }

  //
  // Invoke width callback
  //

  public static <T> void invoke(Service service, String action, AsyncCallback<T> callback) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, callback);
  }
  
  public static <T> void invoke(Service service, String action, AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, callback, timeout);
  }

  public static <T> void invoke(Service service, String action, boolean params, AsyncCallback<T> callback) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback);
  }

  public static <T> void invoke(Service service, String action, boolean params, AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback, timeout);
  }

  public static <T> void invoke(Service service, String action, double params, AsyncCallback<T> callback) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback);
  }

  public static <T> void invoke(Service service, String action, double params, AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback, timeout);
  }
  
  public static <T> void invoke(Service service, String action, int params, AsyncCallback<T> callback) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback);
  }

  public static <T> void invoke(Service service, String action, int params, AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback, timeout);
  }
  
  public static <T> void invoke(Service service, String action, Object params, AsyncCallback<T> callback) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback);
  }

  public static <T> void invoke(Service service, String action, Object params, AsyncCallback<T> callback, long timeout) throws DTalkException {
    DTalk.invoke(service.getTarget(), service.getName(), action, params, callback, timeout);
  }

}

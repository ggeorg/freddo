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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.DTalkChannelClosedEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.util.LOG;

public abstract class FdServiceMgr<T> extends FdService<T> {
  private static final String TAG = LOG.tag(FdServiceMgr.class);
  
  public static final String TYPE = "dtalk.Services";

  private final MessageBusListener<DTalkChannelClosedEvent> chClosedEventH = new MessageBusListener<DTalkChannelClosedEvent>() {
    @Override
    public void messageSent(String topic, DTalkChannelClosedEvent message) {
      String channel = message.getName();
      List<String> srvList = new ArrayList<String>();
      for (Map.Entry<String, FdService<T>> e : services.entrySet()) {
        FdService<T> service = e.getValue();
        if (service.refCntMap.containsKey(channel)) {
          srvList.add(e.getKey());
        }
      }
      for (String srvName : srvList) {
        unregisterService(message.getName(), srvName);
      }
    }
  };

  private final Map<String, FdService<T>> services;
  private final Map<String, FdServiceFactory<T>> factories;

  protected FdServiceMgr(T context, JSONObject options) {
    super(context, TYPE, options);
    services = new ConcurrentHashMap<String, FdService<T>>();
    factories = new ConcurrentHashMap<String, FdServiceFactory<T>>();
    MessageBus.subscribe(DTalkChannelClosedEvent.class.getName(), chClosedEventH);
  }

  protected void registerService(FdServiceFactory<T> factory) {
    factories.put(factory.getType(), factory);
  }

  /**
   * GET SERVICES request handler.
   * 
   * @param request the request.
   */
  public void getServices(JSONObject request) {
    sendResponse(request, getServices());
  }

  private JSONArray getServices() {
    LOG.v(TAG, ">>> getServices");
    JSONArray result = new JSONArray();
    for (String srvName : factories.keySet()) {
      try {
        JSONObject service = new JSONObject();
        service.put("name", srvName);
        service.put("active", services.containsKey(srvName));
        result.put(service);
      } catch (JSONException e) {
        LOG.e(TAG, e.getMessage());
      }
    }
    return result;
  }

  public void doStart(JSONObject request) {
    LOG.v(TAG, ">>> doStart: %s", request);
    final String name = request.optString(MessageEvent.KEY_BODY_PARAMS, null);
    sendResponse(request, (name != null) ? registerService(request.optString("from"), name) : false);
  }

  public void doStop(JSONObject notification) {
    LOG.v(TAG, ">>> doStop: %s", notification);
    final String name = notification.optString(MessageEvent.KEY_BODY_PARAMS, null);
    if (name != null) {
      unregisterService(notification.optString("from"), name);
    }
  }

  private boolean registerService(String from, String name) {
    LOG.v(TAG, ">>> registerService: %s", name);

    FdService<T> service = services.get(name);
    if (service != null) {
      LOG.d(TAG, "Service '%s' already started", name);
      service.refCntMap.put(from, from);
      return true;
    }

    FdServiceFactory<T> factory = factories.get(name);
    if (factory != null) {
      service = factory.create(getContext(), new JSONObject()); // options???
      if (service != null) {
        services.put(name, service);
        service.refCntMap.put(from, from);
        service.start();
        return true;
      } else {
        LOG.w(TAG, "Failed to start service: %s", name);
      }
    } else {
      LOG.w(TAG, "Service factory not found: %s", name);
    }

    return false;
  }

  private void unregisterService(String from, String name) {
    LOG.v(TAG, ">>> unregisterService: (%s) %s", from, name);

    FdService<T> service = services.get(name);
    if (service != null) {
      service.refCntMap.remove(from);
      if (service.refCntMap.size() == 0) {
        services.remove(name);
        service.dispose();
      }
    }
  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Iterator<Map.Entry<String, FdService<T>>> serviceIter = services.entrySet().iterator();
        while (serviceIter.hasNext()) {
          FdService<T> service = serviceIter.next().getValue();
          serviceIter.remove();
          service.dispose();
        }
      }
    });

    MessageBus.unsubscribe(DTalkChannelClosedEvent.class.getName(), chClosedEventH);
  }

}
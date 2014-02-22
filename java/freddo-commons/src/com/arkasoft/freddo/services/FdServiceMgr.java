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
package com.arkasoft.freddo.services;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.arkasoft.freddo.util.LOG;

import freddo.dtalk.events.MessageEvent;

public abstract class FdServiceMgr<T> extends FdService<T> {
  private static final String TAG = LOG.tag(FdServiceMgr.class);

  private final Map<String, FdService<T>> services;
  private final Map<String, FdServiceFactory<T>> factories;

  public FdServiceMgr(T context, JSONObject options) {
    super(context, "dtalk.Services", options);
    services = new ConcurrentHashMap<String, FdService<T>>();
    factories = new ConcurrentHashMap<String, FdServiceFactory<T>>();
  }
  
  protected void registerService(FdServiceFactory<T> factory) {
    factories.put(factory.getType(), factory);
  }

  @Override
  public synchronized void messageSent(final String topic, final JSONObject message) {
    LOG.v(TAG, ">>> onMessage: %s", message.toString());

    try {
      String action = message.optString(MessageEvent.KEY_BODY_ACTION);

      LOG.d(TAG, "Action: %s", action);

      if ("start".equals(action)) {
        String name = message.optString(MessageEvent.KEY_BODY_PARAMS);
        if (name != null) {
          registerService(name);
          return;
        }
      } else if ("stop".equals(action)) {
        String name = message.optString(MessageEvent.KEY_BODY_PARAMS);
        if (name != null) {
          unregisterService(name);
          return;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    super.messageSent(topic, message);
  }

  private void registerService(String name) {
    LOG.v(TAG, ">>> registerService: %s", name);
    if (!services.containsKey(name)) {
      FdServiceFactory<T> factory = factories.get(name);
      if (factory != null) {
        FdService<T> service = factory.create(getContext(), new JSONObject()); // ???
        if (service != null) {
          services.put(name, service);
        } else {
          LOG.w(TAG, "Failed to start service: %s", name);
        }
      }
    }
  }

  private void unregisterService(String name) {
    LOG.v(TAG, ">>> unregisterService: %s", name);
    FdService<T> service = services.remove(name);
    if (service != null) {
      service.dispose();
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
  }

}

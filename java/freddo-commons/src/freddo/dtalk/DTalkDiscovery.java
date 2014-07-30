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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;

import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfDiscoveryListener;
import freddo.dtalk.zeroconf.ZConfManager;
import freddo.dtalk.zeroconf.ZConfServiceInfo;
import freddo.dtalk.zeroconf.ZConfResolveListener;

class DTalkDiscovery {
  private static final String TAG = LOG.tag(DTalkDiscovery.class);

  final Map<String, ZConfServiceInfo> mServiceInfoMap;

  private DTalkDiscoveryListener mDiscoveryListener = null;

  public DTalkDiscovery() {
    mServiceInfoMap = new ConcurrentHashMap<String, ZConfServiceInfo>();
  }

  public void startup() {
    LOG.v(TAG, ">>> startup");
    updateServiceListener(new DTalkDiscoveryListener());
  }

  public void shutdown() {
    LOG.v(TAG, ">>> shutdown");
    updateServiceListener(null);
  }

  private void updateServiceListener(final DTalkDiscoveryListener serviceListener) {
    final ZConfManager nsd = DTalkService.getInstance().getConfiguration().getNsdManager();

    // Remove listener...
    if (mDiscoveryListener != null) {
      LOG.v(TAG, "removeServiceListener: %s", DTalk.SERVICE_TYPE);
      nsd.stopServiceDiscovery(mDiscoveryListener);
    }

    mDiscoveryListener = serviceListener;

    // Register listener...
    if (mDiscoveryListener != null) {
      LOG.v(TAG, "addServiceListener: %s", DTalk.SERVICE_TYPE);
      nsd.discoverServices(DTalk.SERVICE_TYPE, mDiscoveryListener);
    }
  }

  protected void serviceRemoved(final ZConfServiceInfo info) {
    LOG.v(TAG, ">>> presenceRemoved: %s", info);

    if (info == null) {
      return;
    }

    final ZConfServiceInfo _info = mServiceInfoMap.get(info.getServiceName());
    if (_info != null) {
      if (!_info.equals(info)) {
        return;
      }
    }

    mServiceInfoMap.remove(info.getServiceName());
    if (info != null && !info.getServiceName().equals(DTalkService.getInstance().getLocalServiceInfo().getServiceName())) {
      try {
        JSONObject params = new JSONObject();
        params.put(DTalk.KEY_NAME, info.getServiceName());
        // TODO make params this just a string
        JSONObject jsonMsg = DTalk.createMessage(null, DTalk.SERVICE_DTALK_PRESENCE, DTalk.ACTION_REMOVED, params);
        MessageBus.sendMessage(DTalk.SERVICE_DTALK_PRESENCE, jsonMsg);
      } catch (Exception e) {
        LOG.w(TAG, e.getMessage());
      }
    }
  }

  protected void serviceResolved(final ZConfServiceInfo info) {
    LOG.v(TAG, ">>> presenceResolved: %s", info);

    if (info == null /* || !info.hasData() */) {
      LOG.w(TAG, "Presence rejected");
      return;
    }

    String value = info.getTxtRecordValue(DTalk.KEY_PRESENCE_DTALK);
    // not if not dtalk service
    if (!"1".equals(value)) {
      return;
    }

    ZConfServiceInfo _info = mServiceInfoMap.get(info.getServiceName());
    if (_info != null) {
      if (!_info.equals(info)) {
        serviceRemoved(_info);
      } else {
        // already resolved
        return;
      }
    }

    if (info != null && !info.getServiceName().equals(DTalkService.getInstance().getLocalServiceInfo().getServiceName())) {
      // NOTE: self is excluded!!!
      mServiceInfoMap.put(info.getServiceName(), info);

      try {
        JSONObject params = new JSONObject();

        Set<String> pNames = info.getTxtRecord().keySet();
        for (String key : pNames) {
          params.put(key, info.getTxtRecordValue(key));
        }

        params.put(DTalk.KEY_NAME, info.getServiceName());
        params.put(DTalk.KEY_SERVER, info.getHost().getHostAddress());
        params.put(DTalk.KEY_PORT, info.getPort());

        JSONObject jsonMsg = DTalk.createMessage(null, DTalk.SERVICE_DTALK_PRESENCE, DTalk.ACTION_RESOLVED, params);
        MessageBus.sendMessage(DTalk.SERVICE_DTALK_PRESENCE, jsonMsg);
      } catch (Exception e) {
        LOG.w(TAG, e.getMessage());
      }
    }
  }
  
  private class DTalkDiscoveryListener implements ZConfDiscoveryListener {
    @Override
    public void onServiceFound(ZConfServiceInfo serviceInfo) {
      LOG.v(TAG, ">>> onServiceFound: %s", serviceInfo.getServiceName());
      DTalkService.getInstance().getConfiguration().getNsdManager().resolveService(serviceInfo, mResolveListener);
    }

    @Override
    public void onServiceLost(ZConfServiceInfo serviceInfo) {
      LOG.v(TAG, ">>> onServiceLost: %s", serviceInfo.getServiceName());
      DTalkDiscovery.this.serviceRemoved(serviceInfo);
    }
  }
  
  private final ZConfResolveListener mResolveListener = new ZConfResolveListener() {
    @Override
    public void onResolveFailed(ZConfServiceInfo serviceInfo, int errorCode) {
      LOG.e(TAG, ">>> onResolveFailed: %s", serviceInfo.getServiceName());
    }

    @Override
    public void onServiceResolved(ZConfServiceInfo serviceInfo) {
      LOG.v(TAG, ">>> onServiceResolved: %s", serviceInfo.getServiceName());
      DTalkDiscovery.this.serviceResolved(serviceInfo);
    }
  };

}

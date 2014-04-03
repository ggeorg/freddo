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

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.jmdns.ServiceEvent;
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.jmdns.ServiceListener;
import com.arkasoft.freddo.jmdns.impl.constants.DNSConstants;
import com.arkasoft.freddo.messagebus.MessageBus;

import freddo.dtalk.util.LOG;

public class DTalkDiscovery {
  private static final String TAG = LOG.tag(DTalkDiscovery.class);

  private final Map<String, ServiceInfo> serviceInfoMap;

  private final DTalkService dtalkService;
  private DTalkServiceListener serviceListener = null;

  public DTalkDiscovery(DTalkService dtalkService) {
    this.dtalkService = dtalkService;
    this.serviceInfoMap = new ConcurrentHashMap<String, ServiceInfo>();
  }

  public Map<String, ServiceInfo> getServiceInfoMap() {
    return serviceInfoMap;
  }

  public void startup() {
    LOG.v(TAG, ">>> startup");

    setServiceListener(new DTalkServiceListener());
  }

  public void shutdown() {
    LOG.v(TAG, ">>> shutdown");

    setServiceListener(null);
  }

  private void setServiceListener(final DTalkServiceListener serviceListener) {
    JmDNS jmDNS = dtalkService.getConfig().getJmDNS();

    // Remove listener for services of a given type.
    if (this.serviceListener != null) {
      LOG.v(TAG, "removeServiceListener: %s", DTalk.SERVICE_TYPE);
      jmDNS.removeServiceListener(DTalk.SERVICE_TYPE, this.serviceListener);
    }

    this.serviceListener = serviceListener;

    // Register a service.
    if (this.serviceListener != null) {
      LOG.v(TAG, "addServiceListener: %s", DTalk.SERVICE_TYPE);
      jmDNS.addServiceListener(DTalk.SERVICE_TYPE, this.serviceListener);
    }
  }

  protected void serviceRemoved(final ServiceInfo info) {
    LOG.v(TAG, ">>> presenceRemoved: %s", info);

    if (info == null) {
      return;
    }

    ServiceInfo _info = getServiceInfoMap().get(info.getName());
    if (_info != null) {
      if (!_info.equals(info)) {
        return;
      }
    }

    getServiceInfoMap().remove(info.getName());
    if (info != null && !info.getName().equals(dtalkService.getLocalServiceInfo().getName())) {
      try {
        JSONObject params = new JSONObject();
        params.put(DTalk.KEY_NAME, info.getName());
        // TODO make params this just a string
        JSONObject jsonMsg = DTalk.createMessage(null, DTalk.SERVICE_DTALK_PRESENCE, DTalk.ACTION_REMOVED, params);
        MessageBus.sendMessage(DTalk.SERVICE_DTALK_PRESENCE, jsonMsg);
      } catch (Exception e) {
        LOG.w(TAG, e.getMessage());
      }
    }
  }

  protected void serviceResolved(final ServiceInfo info) {
    LOG.v(TAG, ">>> presenceResolved: %s", info);

    if (info == null /* || !info.hasData() */) {
      LOG.w(TAG, "Presence rejected");
      return;
    }

    String value = info.getPropertyString(DTalk.KEY_PRESENCE_DTALK);
    // not if not dtalk service
    if (!"1".equals(value)) {
      return;
    }

    ServiceInfo _info = getServiceInfoMap().get(info.getName());
    if (_info != null) {
      if (!_info.equals(info)) {
        serviceRemoved(_info);
      } else {
        // already resolved
        return;
      }
    }

    if (info != null && !info.getName().equals(dtalkService.getLocalServiceInfo().getName())) {
      // NOTE: self is excluded!!!
      getServiceInfoMap().put(info.getName(), info);

      try {
        JSONObject params = new JSONObject();

        Enumeration<String> pNames = info.getPropertyNames();
        while (pNames.hasMoreElements()) {
          String key = pNames.nextElement();
          params.put(key, info.getPropertyString(key));
        }

        params.put(DTalk.KEY_NAME, info.getName());
        params.put(DTalk.KEY_SERVER, DTalkService.getAddress(info));
        params.put(DTalk.KEY_PORT, info.getPort());

        JSONObject jsonMsg = DTalk.createMessage(null, DTalk.SERVICE_DTALK_PRESENCE, DTalk.ACTION_RESOLVED, params);
        MessageBus.sendMessage(DTalk.SERVICE_DTALK_PRESENCE, jsonMsg);
      } catch (Exception e) {
        LOG.w(TAG, e.getMessage());
      }
    }
  }

  /**
   * Listener for service updates implementation.
   */
  private class DTalkServiceListener implements ServiceListener {
    private final String TAG = LOG.tag(DTalkServiceListener.class);

    @Override
    public void serviceAdded(final ServiceEvent event) {
      LOG.v(TAG, ">>> serviceAdded: %s", event/* .getName() */);

      // check if already resolved
      if (!checkIfAlreadyResolved(event.getInfo())) {
        // we need to resolve
        dtalkService.getConfig().getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            JmDNS jmDNS = dtalkService.getConfig().getJmDNS();

            LOG.d(TAG, "(1) Request service info: %s", event.getName());
            jmDNS.requestServiceInfo(DTalk.SERVICE_TYPE, event.getName(), true);

            LOG.d(TAG, "(2) request service info: %s", event.getName());
            jmDNS.requestServiceInfo(DTalk.SERVICE_TYPE, event.getName(), true, DNSConstants.SERVICE_INFO_TIMEOUT * 2);

            LOG.d(TAG, "(3) request service info: %s", event.getName());
            jmDNS.requestServiceInfo(DTalk.SERVICE_TYPE, event.getName(), true, DNSConstants.SERVICE_INFO_TIMEOUT * 3);

            ServiceInfo info = jmDNS.getServiceInfo(DTalk.SERVICE_TYPE, event.getName(), true);
            if (!checkIfAlreadyResolved(info)) {
              LOG.w(TAG, "Failed to resolved event: %s", event);
            }
          }
        });
      }
    }

    /** A service has been removed. */
    @Override
    public void serviceRemoved(final ServiceEvent event) {
      LOG.v(TAG, ">>> serviceRemoved: %s", event.getName());
      DTalkDiscovery.this.serviceRemoved(event.getInfo());
    }

    /** A service has been resolved. */
    @Override
    public void serviceResolved(final ServiceEvent event) {
      LOG.v(TAG, ">>> serviceResolved: %s", event);
      // check if already resolved
      if (!checkIfAlreadyResolved(event.getInfo())) {
        LOG.w(TAG, "Unresolved event: %s", event);
      }
    }
  }

  protected boolean checkIfAlreadyResolved(ServiceInfo info) {
    if (info != null) {
      InetAddress[] addresses = info.getInetAddresses();
      if (addresses != null && addresses.length > 0) {
        for (InetAddress address : addresses) {
          StringBuilder buf = new StringBuilder();
          buf.append(address);
          buf.append(':');
          buf.append(info.getPort());
          buf.append(' ');
          LOG.d(TAG, "address: %s", buf);
        }
        DTalkDiscovery.this.serviceResolved(info);
        return true;
      }
    }
    return false;
  }

}

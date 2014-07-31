package com.arkasoft.freddo.dtalk.zeroconf.jmdns;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.jmdns.ServiceEvent;
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.jmdns.ServiceListener;

import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfDiscoveryListener;
import freddo.dtalk.zeroconf.ZConfManager;
import freddo.dtalk.zeroconf.ZConfRegistrationListener;
import freddo.dtalk.zeroconf.ZConfResolveListener;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

/**
 * JmDNS implementation of ZConfManager.
 */
public class JmDNSManagerImpl implements ZConfManager {
  private static final String TAG = LOG.tag(JmDNSManagerImpl.class);

  /* convert from ZConfServiceInfo to JmDNS ServiceInfo. */
  private static ServiceInfo convertToJmDNS(ZConfServiceInfo serviceInfo) {
    return ServiceInfo.create(serviceInfo.getServiceType(), serviceInfo.getServiceName(), serviceInfo.getPort(), 0, 0, serviceInfo.getTxtRecord());
  }

  /* convert from JmDNS ServiceInfo to ZConfServiceInfo. */
  private static ZConfServiceInfo convertFromJmDNS(ServiceInfo jmDNSServiceInfo, ZConfServiceInfo serviceInfo) {
    Map<String, String> txtRecord = new HashMap<String, String>();
    Enumeration<String> keys = jmDNSServiceInfo.getPropertyNames();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      txtRecord.put(key, jmDNSServiceInfo.getPropertyString(key));
    }

    serviceInfo.setServiceName(jmDNSServiceInfo.getName());
    serviceInfo.setServiceType(jmDNSServiceInfo.getType());
    serviceInfo.setTxtRecord(txtRecord);
    serviceInfo.setHost(getAddress(jmDNSServiceInfo));
    serviceInfo.setPort(jmDNSServiceInfo.getPort());

    return serviceInfo;
  }

  // ---------------------------

  private final JmDNS mJmDNS;
  private final ExecutorService mThreadPool;

  private Map<ZConfDiscoveryListener, DiscoveryListenerImpl> mDiscoveryListeners = null;
  private Map<ZConfRegistrationListener, RegistrationListenerImpl> mRegistrationListeners = null;

  public JmDNSManagerImpl(JmDNS jmdns, ExecutorService threadPool) {
    mJmDNS = jmdns;
    mThreadPool = threadPool;
  }

  @Override
  public void discoverServices(String serviceType, ZConfDiscoveryListener listener) {
    LOG.v(TAG, ">>> discoverServices: %s", serviceType);

    if (mDiscoveryListeners == null) {
      mDiscoveryListeners = new ConcurrentHashMap<ZConfDiscoveryListener, DiscoveryListenerImpl>();
    }

    if (mDiscoveryListeners.containsKey(listener)) {
      throw new IllegalStateException("Listener already used");
    }

    DiscoveryListenerImpl discoveryListener = new DiscoveryListenerImpl(serviceType, listener);
    mDiscoveryListeners.put(listener, discoveryListener);
    mJmDNS.addServiceListener(serviceType, discoveryListener);
  }

  @Override
  public void stopServiceDiscovery(ZConfDiscoveryListener listener) {
    LOG.v(TAG, ">>> stopServiceDiscovery");

    if (mDiscoveryListeners == null) {
      LOG.d(TAG, "Discovery listener lists is null");
      return;
    }

    DiscoveryListenerImpl serviceListener = mDiscoveryListeners.remove(listener);
    if (serviceListener != null) {
      LOG.d(TAG, "Removing service listener: %s", serviceListener.getServiceType());
      mJmDNS.removeServiceListener(serviceListener.getServiceType(), serviceListener);
    }
  }

  @Override
  public void registerService(ZConfServiceInfo serviceInfo, ZConfRegistrationListener listener) {
    LOG.v(TAG, ">>> registerService: %s", serviceInfo);

    if (mRegistrationListeners == null) {
      mRegistrationListeners = new ConcurrentHashMap<ZConfRegistrationListener, RegistrationListenerImpl>();
    }

    if (mRegistrationListeners.containsKey(listener)) {
      throw new IllegalStateException("Listener already registered.");
    }

    final ServiceInfo jmDNSServiceInfo = convertToJmDNS(serviceInfo);
    RegistrationListenerImpl registrationListener = new RegistrationListenerImpl(jmDNSServiceInfo, listener);
    mRegistrationListeners.put(listener, registrationListener);

    try {
      LOG.d(TAG, "ServiceInfo before: %s", jmDNSServiceInfo);

      // Note that the given {@code ServiceInfo} is bound to this {@code JmDNS}
      // instance, and should not be reused for any other {@linkplain
      // #registerService(ServiceInfo)}.
      mJmDNS.registerService(jmDNSServiceInfo);

      // Note: don't create new one, just copy all info to ZConfServiceInfo
      listener.onServiceRegistered(convertFromJmDNS(jmDNSServiceInfo, serviceInfo));

    } catch (IOException e) {
      LOG.e(TAG, "Service registration failed.", e);

      // Inform listener about failure.
      listener.onRegistrationFailed(serviceInfo, -1);
    }
  }

  @Override
  public void unregisterService(ZConfRegistrationListener listener) {
    LOG.v(TAG, ">>> unregisterService");

    if (mRegistrationListeners == null) {
      LOG.d(TAG, "Registration listener list is null.");
      return;
    }

    RegistrationListenerImpl registrationListener = mRegistrationListeners.remove(listener);
    if (registrationListener != null) {
      LOG.d(TAG, "Unregister service: %s", registrationListener.getServiceInfo());

      // Note: ServiceInfo must be the one used in:
      // mJmDNS.registerService(jmDNSServiceInfo);
      mJmDNS.unregisterService(registrationListener.getServiceInfo());

      // Inform listener...
      // listener.onServiceUnregistered(registrationListener.get);
    }
  }

  @Override
  public void resolveService(final ZConfServiceInfo serviceInfo, final ZConfResolveListener listener) {
    LOG.v(TAG, ">>> resolveService: %s", serviceInfo);

    mThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        ServiceInfo jmDNSServiceInfo = mJmDNS.getServiceInfo(serviceInfo.getServiceType(), serviceInfo.getServiceName());
        if (jmDNSServiceInfo == null) {
          listener.onResolveFailed(serviceInfo, -1);
        } else {
          // Note: don't create new one, just copy all info to ZConfServiceInfo
          listener.onServiceResolved(convertFromJmDNS(jmDNSServiceInfo, serviceInfo));
        }
      }
    });
  }

  private static InetAddress getAddress(ServiceInfo info) {
    // try {
    // return InetAddress.getByName(info.getServer());
    // } catch (UnknownHostException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }

    InetAddress[] addresses = info.getInetAddresses();
    if (addresses.length > 0) {
      for (InetAddress address : addresses) {
        if (!address.isAnyLocalAddress()) {
          return address;
        }
      }
    }

    return null;
  }

  /** Listener for service updates */
  private static class DiscoveryListenerImpl implements ServiceListener {
    private static final String TAG = LOG.tag(DiscoveryListenerImpl.class);

    private final String mServiceType;
    private final ZConfDiscoveryListener mListener;

    DiscoveryListenerImpl(String serviceType, ZConfDiscoveryListener listener) {
      mServiceType = serviceType;
      mListener = listener;
    }

    String getServiceType() {
      return mServiceType;
    }

    @SuppressWarnings("unused")
    ZConfDiscoveryListener getListener() {
      return mListener;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
      LOG.v(TAG, ">>> serviceAdded: %s", event.getName());
      mListener.onServiceFound(new ZConfServiceInfo(event.getName(), event.getType()));
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
      LOG.v(TAG, ">>> serviceRemoved: %s", event.getName());
      mListener.onServiceLost(new ZConfServiceInfo(event.getName(), event.getType()));
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
      LOG.v(TAG, ">>> serviceResolved: %s", event.getInfo());
    }

  }

  private static class RegistrationListenerImpl implements ServiceListener {
    @SuppressWarnings("unused")
    private static final String TAG = LOG.tag(RegistrationListenerImpl.class);

    private ServiceInfo mServiceInfo;
    private final ZConfRegistrationListener mListener;

    RegistrationListenerImpl(ServiceInfo info, ZConfRegistrationListener listener) {
      mServiceInfo = info;
      mListener = listener;
    }

    ServiceInfo getServiceInfo() {
      return mServiceInfo;
    }

    void setServiceInfo(ServiceInfo serviceInfo) {
      mServiceInfo = serviceInfo;
    }

    @SuppressWarnings("unused")
    ZConfRegistrationListener getListener() {
      return mListener;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
      // TODO Auto-generated method stub

    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
      // TODO Auto-generated method stub

    }

    @Override
    public void serviceResolved(ServiceEvent event) {
      // TODO Auto-generated method stub

    }

  }

}

package com.arkasoft.freddo.nsd.jmdns;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.jmdns.ServiceEvent;
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.jmdns.ServiceListener;

import freddo.dtalk.nsd.DiscoveryListener;
import freddo.dtalk.nsd.NsdManager;
import freddo.dtalk.nsd.NsdServiceInfo;
import freddo.dtalk.nsd.RegistrationListener;
import freddo.dtalk.nsd.ResolveListener;
import freddo.dtalk.util.LOG;

public class NsdManagerImpl implements NsdManager {
  private static final String TAG = LOG.tag(NsdManagerImpl.class);

  private final JmDNS mJmdns;
  private final Executor mThreadPool;

  private Map<DiscoveryListener, JmDNSDiscoveryListener> mDiscoveryListeners = null;
  private Map<RegistrationListener, JmDNSRegistrationListener> mRegistrationListeners = null;

  protected NsdManagerImpl(JmDNS jmdns, Executor threadPool) {
    mJmdns = jmdns;
    mThreadPool = threadPool;
  }

  @Override
  public void discoverServices(String serviceType, DiscoveryListener listener) {
    if (mDiscoveryListeners == null) {
      mDiscoveryListeners = new ConcurrentHashMap<DiscoveryListener, JmDNSDiscoveryListener>();
    }
    if (mDiscoveryListeners.containsKey(listener)) {
      throw new IllegalStateException("Listener already used");
    }
    JmDNSDiscoveryListener discoveryListener = new JmDNSDiscoveryListener(serviceType, listener);
    mDiscoveryListeners.put(listener, discoveryListener);
    mJmdns.addServiceListener(serviceType, discoveryListener);
  }

  @Override
  public void stopServiceDiscovery(DiscoveryListener listener) {
    if (mDiscoveryListeners == null) {
      return;
    }
    JmDNSDiscoveryListener serviceListener = mDiscoveryListeners.remove(listener);
    if (serviceListener != null) {
      mJmdns.removeServiceListener(serviceListener.getServiceType(), serviceListener);
    }
  }

  @Override
  public void registerService(NsdServiceInfo serviceInfo, RegistrationListener listener) {
    if (mRegistrationListeners == null) {
      mRegistrationListeners = new ConcurrentHashMap<RegistrationListener, JmDNSRegistrationListener>();
    }
    if (mRegistrationListeners.containsKey(listener)) {
      throw new IllegalStateException("Listener already used");
    }
    ServiceInfo info = ServiceInfo.create(serviceInfo.getServiceType(), serviceInfo.getServiceName(), serviceInfo.getPort(), 0, 0, serviceInfo.getTxtRecord());
    JmDNSRegistrationListener registrationListener = new JmDNSRegistrationListener(info, listener);
    mRegistrationListeners.put(listener, registrationListener);
    try {
      mJmdns.registerService(info);
    } catch (IOException e) {
      listener.onRegistrationFailed(serviceInfo, FAILURE_INTERNAL_ERROR);
    }
  }

  @Override
  public void unregisterService(RegistrationListener listener) {
    if (mRegistrationListeners == null) {
      return;
    }
    JmDNSRegistrationListener registrationListener = mRegistrationListeners.remove(listener);
    if (registrationListener != null) {
      mJmdns.unregisterService(registrationListener.getServiceInfo());
    }
  }

  @Override
  public void resolveService(final NsdServiceInfo serviceInfo, final ResolveListener listener) {
    mThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        mJmdns.getServiceInfo(serviceInfo.getServiceType(), serviceInfo.getServiceName());
      }
    });
  }

  /** Listener for service updates */
  private class JmDNSDiscoveryListener implements ServiceListener {
    private final String mServiceType;
    private final DiscoveryListener mListener;

    JmDNSDiscoveryListener(String serviceType, DiscoveryListener listener) {
      mServiceType = serviceType;
      mListener = listener;
    }

    public String getServiceType() {
      return mServiceType;
    }

    public DiscoveryListener getListener() {
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

  private class JmDNSRegistrationListener implements ServiceListener {
    private final ServiceInfo mServiceInfo;
    private final RegistrationListener mListener;

    JmDNSRegistrationListener(ServiceInfo info, RegistrationListener listener) {
      mServiceInfo = info;
      mListener = listener;
    }

    public ServiceInfo getServiceInfo() {
      return mServiceInfo;
    }

    public RegistrationListener getListener() {
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

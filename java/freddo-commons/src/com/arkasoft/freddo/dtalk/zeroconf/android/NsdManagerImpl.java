package com.arkasoft.freddo.dtalk.zeroconf.android;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfDiscoveryListener;
import freddo.dtalk.zeroconf.ZConfManager;
import freddo.dtalk.zeroconf.ZConfRegistrationListener;
import freddo.dtalk.zeroconf.ZConfServiceInfo;
import freddo.dtalk.zeroconf.ZConfResolveListener;

/**
 * 
 * Implementation has still problems see:
 * 
 * https://code.google.com/p/android/issues/detail?id=35810
 * 
 */
public class NsdManagerImpl implements ZConfManager {
  private static final String TAG = LOG.tag(NsdManagerImpl.class);
  
  /** @see: https://code.google.com/p/android/issues/detail?id=35810 */
  public static final boolean ISSUE35810_RESOLVED = false;

  /* convert from ZConfServiceInfo to NsdServiceInfo. */
  private static NsdServiceInfo convertToAndroid(ZConfServiceInfo serviceInfo) {
    NsdServiceInfo info = new NsdServiceInfo();
    info.setServiceName(serviceInfo.getServiceName());
    info.setServiceType(serviceInfo.getServiceType());
    info.setHost(serviceInfo.getHost());
    info.setPort(serviceInfo.getPort());
    Set<String> keys = serviceInfo.getTxtRecord().keySet();
    for (String key : keys) {
      info.getAttributes().put(key, serviceInfo.getTxtRecordValue(key).getBytes());
    }
    return info;
  }

  /* convert from NsdServiceInfo to ZConfServiceInfo. */
  private static ZConfServiceInfo convertFromAndroid(NsdServiceInfo service) {
    Map<String, String> txtRecord = new HashMap<String, String>();
    for (Map.Entry<String, byte[]> entry : service.getAttributes().entrySet()) {
      txtRecord.put(entry.getKey(), new String(entry.getValue()));
    }
    return new ZConfServiceInfo(service.getServiceName(), service.getServiceType(), 
        txtRecord, service.getHost(), service.getPort());
  }

  // ---------------------------

  private final NsdManager mNsdManager;

  private Map<ZConfDiscoveryListener, DiscoveryListenerImpl> mDiscoveryListeners = null;
  private Map<ZConfRegistrationListener, RegistrationListenerImpl> mRegistrationListeners = null;

  public NsdManagerImpl(NsdManager nsd) {
    mNsdManager = nsd;
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
    mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
  }

  @Override
  public void stopServiceDiscovery(ZConfDiscoveryListener listener) {
    LOG.v(TAG, ">>> stopServiceDiscovery");

    if (mDiscoveryListeners == null) {
      return;
    }
    DiscoveryListenerImpl serviceListener = mDiscoveryListeners.remove(listener);
    if (serviceListener != null) {
      mNsdManager.stopServiceDiscovery(serviceListener);
    }
  }

  @Override
  public void registerService(ZConfServiceInfo serviceInfo, ZConfRegistrationListener listener) {
    LOG.v(TAG, ">>> registerService: %s", serviceInfo);

    if (mRegistrationListeners == null) {
      mRegistrationListeners = new ConcurrentHashMap<ZConfRegistrationListener, RegistrationListenerImpl>();
    }
    if (mRegistrationListeners.containsKey(listener)) {
      throw new IllegalStateException("Listener already used");
    }

    final NsdServiceInfo info = convertToAndroid(serviceInfo);
    RegistrationListenerImpl registrationListener = new RegistrationListenerImpl(info, listener);
    mRegistrationListeners.put(listener, registrationListener);
    mNsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
  }

  @Override
  public void unregisterService(ZConfRegistrationListener listener) {
    if (mRegistrationListeners == null) {
      return;
    }
    RegistrationListenerImpl registrationListener = mRegistrationListeners.remove(listener);
    if (registrationListener != null) {
      mNsdManager.unregisterService(registrationListener);
    }
  }

  @Override
  public void resolveService(final ZConfServiceInfo serviceInfo, final ZConfResolveListener listener) {
    mNsdManager.resolveService(convertToAndroid(serviceInfo), new NsdManager.ResolveListener() {
      @Override
      public void onServiceResolved(NsdServiceInfo service) {
        listener.onServiceResolved(convertFromAndroid(service));
      }

      @Override
      public void onResolveFailed(NsdServiceInfo service, int errorCode) {
        listener.onResolveFailed(serviceInfo, errorCode);
      }
    });
  }

  /* Listener for service updates */
  private static class DiscoveryListenerImpl implements NsdManager.DiscoveryListener {
    private static final String TAG = LOG.tag(DiscoveryListenerImpl.class);

    private final String mServiceType;
    private final ZConfDiscoveryListener mListener;

    DiscoveryListenerImpl(String serviceType, ZConfDiscoveryListener listener) {
      mServiceType = serviceType;
      mListener = listener;
    }

    @SuppressWarnings("unused")
    String getServiceType() {
      return mServiceType;
    }

    @SuppressWarnings("unused")
    ZConfDiscoveryListener getListener() {
      return mListener;
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
      LOG.v(TAG, ">>> onDiscoveryStarted: %s", serviceType);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
      LOG.v(TAG, ">>> onDiscoveryStopped: %s", serviceType);
    }

    @Override
    public void onServiceFound(NsdServiceInfo service) {
      LOG.v(TAG, ">>> onServiceFound: %s", service.getServiceName());
      mListener.onServiceFound(convertFromAndroid(service));
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
      LOG.v(TAG, ">>> onServiceLost: %s", service.getServiceName());
      mListener.onServiceLost(convertFromAndroid(service));
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
      LOG.v(TAG, ">>> onStartDiscoveryFailed: %s", serviceType);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
      LOG.v(TAG, ">>> onStopDiscoveryFailed: %s", serviceType);
    }

  }

  /* Listener for registration updates. */
  private static class RegistrationListenerImpl implements NsdManager.RegistrationListener {
    private static final String TAG = LOG.tag(RegistrationListenerImpl.class);

    private final NsdServiceInfo mServiceInfo;
    private final ZConfRegistrationListener mListener;

    RegistrationListenerImpl(NsdServiceInfo info, ZConfRegistrationListener listener) {
      mServiceInfo = info;
      mListener = listener;
    }

    @SuppressWarnings("unused")
    NsdServiceInfo getServiceInfo() {
      return mServiceInfo;
    }

    @SuppressWarnings("unused")
    ZConfRegistrationListener getListener() {
      return mListener;
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo service, int errorCode) {
      LOG.e(TAG, ">>> onRegistrationFailed: %s (%d)", service.getServiceName(), errorCode);
      mListener.onRegistrationFailed(convertFromAndroid(service), errorCode);
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo service) {
      LOG.i(TAG, ">>> onServiceRegistered: %s", service.toString());
      mListener.onServiceRegistered(convertFromAndroid(service));
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo service) {
      LOG.i(TAG, ">>> onServiceUnregistered: %s", service);
      mListener.onServiceUnregistered(convertFromAndroid(service));
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo service, int errorCode) {
      LOG.i(TAG, ">>> onUnregistrationFailed: %s (%d)", service.getServiceName(), errorCode);
      mListener.onUnregistrationFailed(convertFromAndroid(service), errorCode);
    }

  }
}

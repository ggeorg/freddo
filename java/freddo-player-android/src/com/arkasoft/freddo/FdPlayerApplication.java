package com.arkasoft.freddo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.nsd.NsdServiceInfo;
import freddo.dtalk.util.AndroidLogger;
import freddo.dtalk.util.LOG;

public class FdPlayerApplication extends Application {
  private static final String TAG = LOG.tag(FdPlayerApplication.class);

  static {
    LOG.setLogger(new AndroidLogger());
    LOG.setLogLevel(LOG.VERBOSE);
  }

  private final MessageBusListener<DTalkServiceEvent> mDtalkServiceEventHandler = new MessageBusListener<DTalkServiceEvent>() {
    @Override
    public void messageSent(String topic, DTalkServiceEvent message) {
      setServiceInfo(message.getServiceInfo());
    }
  };

  // ------------------------------------------------------------------------

  private final ExecutorService mThreadPool;

  private NsdServiceInfo mServiceInfo = null;

  private WifiManager.MulticastLock multicastLock;

  private JmDNS jmDNS = null;
  private static final Object sJmDNSLock = new Object();

  private final FdServiceConfiguration configuration;

  public FdPlayerApplication() {
    // Create application's thread pool.
    mThreadPool = Executors.newCachedThreadPool();
    
    // Initialize DTalkService
    DTalkService.init(configuration = new FdServiceConfiguration(this));
  }

  public FdServiceConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void onCreate() {
    LOG.v(TAG, ">>> onCreate()");
    super.onCreate();

    // Sets the default values.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    // Subscribe for DTalkServiceEvent, that indicates when the application is
    // ready to get loaded.
    MessageBus.subscribe(DTalkServiceEvent.class.getName(), mDtalkServiceEventHandler);
  }

  @Override
  public void onTerminate() {
    LOG.v(TAG, ">>> onTerminate()");

    // Unsubscribe from DTalkServiceEvent.
    MessageBus.unsubscribe(DTalkServiceEvent.class.getName(), mDtalkServiceEventHandler);

    synchronized (sJmDNSLock) {
      try {
        DTalkService.getInstance().shutdown();
      } catch (Exception e) {
        LOG.e(TAG, e.getMessage());
      } finally {
        // TODO dispose???
      }

      if (jmDNS != null) {
        try {
          LOG.d(TAG, "Stopping JmDNS...");
          jmDNS.close();
        } catch (IOException e) {
          Log.e(TAG, e.getMessage());
        } finally {
          jmDNS = null;
        }
      }
    }

    if (multicastLock != null && multicastLock.isHeld()) {
      LOG.d(TAG, "Releasing multicastLosk");
      multicastLock.release();
    }

    // Shutdown thread pool...
    mThreadPool.shutdown();

    // Always call at the end!!!
    super.onTerminate();
  }

  /**
   * 
   * @see FdServiceConfiguration#getJmDNS()
   */
  protected JmDNS getJmDNS() {
    LOG.v(TAG, ">>> getJmDNS");

    synchronized (sJmDNSLock) {
      if (jmDNS == null) {

        LOG.d(TAG, "Getting wifi manager");
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (multicastLock == null) {
          multicastLock = wifi.createMulticastLock("FTV");
          multicastLock.setReferenceCounted(true);
        } else {
          LOG.d(TAG, "multicastLock != null");
        }

        multicastLock.acquire();

        try {
          WifiInfo wifiinfo = wifi.getConnectionInfo();
          int intaddr = wifiinfo.getIpAddress();

          byte[] byteaddr = new byte[] {(byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff)};
          InetAddress addr = InetAddress.getByAddress(byteaddr);

          if (jmDNS == null) {
            LOG.d(TAG, "Create JmDNS: %s", addr);
            jmDNS = JmDNS.create(addr);
          }
        } catch (IOException e) {
          LOG.e(TAG, "Failed to start JmDNS service!", e);
        }
      }
      return jmDNS;
    }
  }

  protected InetAddress getInetAddress() throws UnknownHostException {
    LOG.d(TAG, "Getting wifi manager");

    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
      Log.d(TAG, "Wifi state is not enabled");
      return null;
    }

    WifiInfo connInfo = wifiManager.getConnectionInfo();
    int ipAddress = connInfo.getIpAddress();

    byte[] byteaddr = new byte[] {
        (byte) (ipAddress & 0xff),
        (byte) (ipAddress >> 8 & 0xff),
        (byte) (ipAddress >> 16 & 0xff),
        (byte) (ipAddress >> 24 & 0xff)};

    return InetAddress.getByAddress(byteaddr);
  }

  /**
   * 
   * @see FdServiceConfiguration#getTargetName()
   */
  protected String getTargetName() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String targetName = sharedPreferences.getString(Constants.PREF_TARGET_NAME, getResources().getString(R.string.app_name));
    return targetName;
  }

  /**
   * 
   * @see FdServiceConfiguration#isWebPresenceEnabled()
   */
  protected boolean isWebPresenceEnabled() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    boolean webPresence = sharedPreferences.getBoolean(Constants.PREF_WEB_PRESENCE, false);
    return webPresence;
  }

  /**
   * 
   * @see FdServiceConfiguration#getWebPresenceURL()
   */
  protected String getWebPresenceURL() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String webPresenceURL = sharedPreferences.getString(Constants.PREF_WEB_PRESENCE_URL, null);
    return webPresenceURL;
  }

  /**
   * 
   * @return FdServiceConfiguration#getThreadPool()
   */
  protected ExecutorService getThreadPool() {
    return mThreadPool;
  }

  NsdServiceInfo getServiceInfo() {
    return mServiceInfo;
  }

  protected void setServiceInfo(NsdServiceInfo serviceInfo) {
    mServiceInfo = serviceInfo;
  }

}

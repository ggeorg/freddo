package com.arkasoft.freddo;

import java.io.IOException;
import java.net.InetAddress;
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
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.util.AndroidLogger;
import freddo.dtalk.util.LOG;

public class FdPlayer extends Application {
  private static final String TAG = LOG.tag(FdPlayer.class);

  static {
    LOG.setLogger(new AndroidLogger());
    LOG.setLogLevel(LOG.VERBOSE);
  }

  private final MessageBusListener<DTalkServiceEvent> dtalkServiceEventHandler = new MessageBusListener<DTalkServiceEvent>() {
    @Override
    public void messageSent(String topic, DTalkServiceEvent message) {
      setServiceInfo(message.getServiceInfo());
    }
  };

  // -----------------------------------------------------------------------

//  private final MessageBusListener<JSONObject> appLauncherEventHandler =
//      new MessageBusListener<JSONObject>() {
//        @Override
//        public void messageSent(String topic, JSONObject message) {
//          // String serviceName = message.optString("serviceName");
//          String action = message.optString("action");
//          JSONObject params = message.optJSONObject("params");
//          if ("launch".equals(action) && params != null && params.has("url")) {
//            String url = params.optString("url");
//            if (url != null) {
//              Intent i = new Intent(Intent.ACTION_VIEW);
//              i.setData(Uri.parse(url + "?ws=" + serviceInfo.getPort()));
//              i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//              i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//              startActivity(i);
//              // TODO wait for web app started and notify sender...
//            }
//          }
//        }
//      };

  // ------------------------------------------------------------------------

  private ExecutorService threadPool;

  private ServiceInfo serviceInfo = null;

  private WifiManager.MulticastLock multicastLock;

  private JmDNS jmDNS = null;
  private static final Object sJmDNSLock = new Object();
  
  private final FdServiceConfiguration configuration;

  public FdPlayer() {
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

    // Create application's thread pool.
    threadPool = Executors.newCachedThreadPool();

    // Sets the default values.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    // Subscribe for DTalkServiceEvent, that indicates when the app is ready to
    // get loaded.
    MessageBus.subscribe(DTalkServiceEvent.class.getName(), dtalkServiceEventHandler);

    // Subscribe for app launcher events
    // MessageBus.subscribe("freddotv.AppLauncher", appLauncherEventHandler);
  }

  @Override
  public void onTerminate() {
    LOG.v(TAG, ">>> onTerminate()");

    MessageBus.unsubscribe(DTalkServiceEvent.class.getName(), dtalkServiceEventHandler);

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

    if (threadPool != null) {
      threadPool.shutdown();
      threadPool = null;
    }

    super.onTerminate();
  }

  synchronized JmDNS getJmDNS() {
    synchronized (sJmDNSLock) {
      if (jmDNS == null) {
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

  String getTargetName() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String targetName = sharedPreferences.getString(Constants.PREF_TARGET_NAME, getResources().getString(R.string.app_name));
    return targetName;
  }

  boolean isWebPresence() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    boolean webPresence = sharedPreferences.getBoolean(Constants.PREF_WEB_PRESENCE, false);
    return webPresence;
  }

  String getWebPresenceURL() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String webPresenceURL = sharedPreferences.getString(Constants.PREF_WEB_PRESENCE_URL, null);
    return webPresenceURL;
  }

  ExecutorService getThreadPool() {
    return threadPool;
  }

  ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  protected void setServiceInfo(ServiceInfo serviceInfo) {
    this.serviceInfo = serviceInfo;
  }

}

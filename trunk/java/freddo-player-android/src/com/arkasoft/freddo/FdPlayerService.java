package com.arkasoft.freddo;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.arkasoft.freddo.dtalk.netty4.server.WebSocketServerHandler;
import com.arkasoft.freddo.utils.AssetFileRequestHandler;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkService.Configuration;
import freddo.dtalk.util.LOG;

@SuppressLint("NewApi")
public class FdPlayerService extends Service implements OnSharedPreferenceChangeListener {
  private static final String TAG = LOG.tag(FdPlayerService.class);

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    LOG.v(TAG, ">>> onCreate");
    super.onCreate();

    // Register shared preference change listener.
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    sharedPrefs.registerOnSharedPreferenceChangeListener(this);

    // Register asset file request handler.
    PackageInfo pInfo = null;
    try {
      pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    } catch (NameNotFoundException e) {
      // Ignore;
    }
    
    WebSocketServerHandler.addRequestHandler("/", new AssetFileRequestHandler(this, "www", pInfo));
    WebSocketServerHandler.addRequestHandler("/remctrl", new AssetFileRequestHandler(this, "www", pInfo));
  }

  @Override
  public void onDestroy() {
    LOG.v(TAG, ">>> onDestroy");

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);

    try {
      stopDTalkService();
    } catch (Exception e) {
      e.printStackTrace();
    }

    super.onDestroy();
    // System.exit(0);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    LOG.v(TAG, ">>> onStart");

    Configuration config = DTalkService.getInstance().getConfiguration();

    // Set required system properties...
    String targetName = config.getTargetName();
    boolean webPresence = config.isWebPresenceEnabled();

    LOG.d(TAG, "TargetName: %s, WebPresence: %b", targetName, webPresence);

    config.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try {
          startDTalkService();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return Service.START_STICKY;
  }

  private synchronized void startDTalkService() {
    LOG.v(TAG, ">>> startDTalkService");

    Configuration config = DTalkService.getInstance().getConfiguration();

    try {
      if (config.getNsdManager() != null) {
        DTalkService.getInstance().startup();
      }
    } catch (Exception e) {
      LOG.e(TAG, "Failed to start services services!", e);
    }
  }

  private synchronized void stopDTalkService() {
    LOG.v(TAG, ">>> stopDTalkService");

    try {
      DTalkService.getInstance().shutdown();
    } catch (Exception e) {
      // Ignore
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    LOG.d(TAG, ">>> onSharedPreferenceChanged: %s", key);

    FdServiceConfiguration config = (FdServiceConfiguration) DTalkService.getInstance().getConfiguration();
    if (key.equalsIgnoreCase(Constants.PREF_TARGET_NAME)) {
      @SuppressWarnings("unused")
      String targetName = config.getTargetName();
      onTargetNameChanged();
    } else if (key.equalsIgnoreCase(Constants.PREF_WEB_PRESENCE) || key.equalsIgnoreCase(Constants.PREF_WEB_PRESENCE_URL)) {
      boolean webPresence = config.isWebPresenceEnabled();
      enableWebPresence(webPresence);
    }
  }

  protected void onTargetNameChanged() {
    LOG.v(TAG, ">>> onTargetNameChanged");

    FdServiceConfiguration config = (FdServiceConfiguration) DTalkService.getInstance().getConfiguration();
    config.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        DTalkService.getInstance().republishService();
      }
    });
  }

  protected void enableWebPresence(final boolean webPresence) {
    LOG.v(TAG, ">>> enableWebPresence: %b", webPresence);

    FdServiceConfiguration config = (FdServiceConfiguration) DTalkService.getInstance().getConfiguration();
    config.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        DTalkService.getInstance().enableWebPresence(webPresence);
      }
    });
  }

}

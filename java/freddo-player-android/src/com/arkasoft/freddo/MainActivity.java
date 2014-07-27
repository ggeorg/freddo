/*
 * Copyright (C) 2013 ArkaSoft, LLC.
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
package com.arkasoft.freddo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;
import com.arkasoft.freddo.services.AndroidFdServiceMgr;

import freddo.dtalk.DTalkException;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.nsd.NsdServiceInfo;
import freddo.dtalk.services.clients.AppView;
import freddo.dtalk.util.LOG;

@SuppressLint({"CutPasteId", "InlinedApi", "NewApi"})
public class MainActivity extends FdPlayerActivity implements OnSharedPreferenceChangeListener {
  private static final String TAG = LOG.tag(MainActivity.class);

  // --------------------------------------------------------------------------
  private static boolean mainActivityIsOpen = false;
  private static final Object mainActivityIsOpenLock = new Object();

  public static boolean mainActivityIsOpen() {
    synchronized (mainActivityIsOpenLock) {
      return mainActivityIsOpen;
    }
  }

  private static void mainActivityIsOpen(boolean b) {
    synchronized (mainActivityIsOpenLock) {
      mainActivityIsOpen = b;
    }
  }

  // --------------------------------------------------------------------------
  private final ExecutorService threadPool = Executors.newCachedThreadPool();

  // this starts the service
  private final Runnable doStartService = new Runnable() {
    @Override
    public void run() {
      LOG.v(TAG, "doStartService#run()");
      Intent serviceIntent = new Intent(MainActivity.this, FdPlayerService.class);
      startService(serviceIntent);
    }
  };

  // this stops the service
  private final Runnable doStopService = new Runnable() {
    @Override
    public void run() {
      LOG.v(TAG, "doStopService#run()");
      Intent serviceIntent = new Intent(MainActivity.this, FdPlayerService.class);
      stopService(serviceIntent);
    }
  };

  // --------------------------------------------------------------------------
  private final MessageBusListener<DTalkServiceEvent> dtalkServiceEventHandler = new MessageBusListener<DTalkServiceEvent>() {
    @Override
    public void messageSent(String topic, DTalkServiceEvent message) {
      setServiceInfo(message.getServiceInfo());
    }
  };

  private NsdServiceInfo mServiceInfo = null;

  // --------------------------------------------------------------------------
  private AndroidFdServiceMgr serviceMgr = null;

  private View root;
  private ProgressBar progressBar;

  public MainActivity() {
    MessageBus.subscribe(DTalkServiceEvent.class.getName(), dtalkServiceEventHandler);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    LOG.v(TAG, ">>> onCreate");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Get root and add a listener for SystemUI visibility.
    root = findViewById(R.id.root);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      root.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
        @SuppressLint("HandlerLeak")
        private final Handler h = new Handler() {
          @Override
          public void handleMessage(Message msg) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
              setFullscreenMode(true);
            }
          }
        };

        @Override
        public void onSystemUiVisibilityChange(int visibility) {
          if ((visibility == View.SYSTEM_UI_FLAG_VISIBLE) &&
              (getResources().getConfiguration().orientation ==
              Configuration.ORIENTATION_LANDSCAPE)) {
            h.removeMessages(0);
            h.sendEmptyMessageDelayed(0, 3333L);
          } else {
            // Do other stuff here..resume the video/game?
          }
        }
      });
    }

    // Set in fullscreen mode if orientation=landscape...
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setFullscreenMode(true);
    }

    // Register MainActivity as SharedPreferenceListener
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    // Start DTalk Service...
    boolean startAtBootTime = sharedPreferences.getBoolean(Constants.PREF_AUTO_STARTUP, false);
    if (!startAtBootTime) {
      threadPool.execute(doStartService);
    }

    // Options that we pass to the services...
    // Could have initial values we retrieve from preferences.
    JSONObject options = new JSONObject();

    // Setup progress bar
    progressBar = (ProgressBar) findViewById(R.id.progressBar);

    // Start service manager...
    if (serviceMgr == null) {
      serviceMgr = new AndroidFdServiceMgr(this, options);
      serviceMgr.start();
    }

    // Start the spinner...
    // spinnerStart("", "Loading...");

    FdPlayerApplication app = (FdPlayerApplication) getApplication();
    if (app.getServiceInfo() != null) {
      mServiceInfo = app.getServiceInfo();
      loadUrl();
    }
  }

  @Override
  protected ProgressBar getProgressBar() {
    return progressBar;
  }

  private void loadUrl() {
    LOG.v(TAG, ">>> loadUrl");
    if (mServiceInfo != null) {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
      // final String url = sharedPrefs.getString("pref_url",
      // "http://dev.arkasoft.com/fd-test-api/index.html");
      loadUrl(sharedPrefs.getString("pref_url", "about:blank"));
    }
  }

  private void loadUrl(String url) {
    LOG.d(TAG, ">>> loadUrl: %s", url);
    try {
      ensureAppView().setUrl(url);
    } catch (Exception e) {
      LOG.e(TAG, e.getMessage());
    }
  }

  // FIXME fix project with targetName change
  protected void setServiceInfo(NsdServiceInfo serviceInfo) {
    if (mServiceInfo != serviceInfo) {
      mServiceInfo = serviceInfo;
      loadUrl();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    // don't reload the current page when the orientation is changed
    super.onConfigurationChanged(newConfig);

    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setFullscreenMode(true);
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      setFullscreenMode(false);
    }
  }

  protected void setFullscreenMode(boolean fullscreen) {
    WindowManager.LayoutParams attrs = getWindow().getAttributes();
    if (fullscreen) {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            // | View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
      } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LOW_PROFILE
            // | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            // | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            // | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            );
      }
    } else {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
        // This snippet shows the system bars. It does this by removing all the
        // flags except for the ones that make the content appear under the
        // system bars.
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            // | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // | View.SYSTEM_UI_FLAG_VISIBLE
            );
      }
    }
  }

  @Override
  protected void onPause() {
    LOG.d(TAG, ">>> onPause");
    mainActivityIsOpen(false); // XXX ???

    AppView appView = ensureAppView();
    // TODO appView.pause();

    super.onPause();
  }

  @Override
  protected void onResume() {
    LOG.d(TAG, ">>> onResume");
    super.onResume();

    AppView appView = ensureAppView();
    // TODO appView.resume();

    mainActivityIsOpen(true); // XXX ???
  }

  @Override
  public void onDestroy() {
    LOG.d(TAG, ">>> onDestroy");

    super.onDestroy();

    if (serviceMgr != null) {
      serviceMgr.dispose();
      serviceMgr = null;
    }

    // Stop service...
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    boolean startAtBootTime = sharedPreferences.getBoolean(Constants.PREF_AUTO_STARTUP, false);
    if (!startAtBootTime) {
      threadPool.execute(doStopService);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.action_airplay_reload: {
        try {
          ensureAppView().reload(true);
        } catch (DTalkException e) {
          LOG.e(TAG, e.getMessage());
        }
        break;
      }
      case R.id.action_airplay_reset: {
        Intent intent = new Intent("action_airplay_reset");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        break;
      }
      case R.id.action_settings: {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, FdPreferencesActivity.class);
        startActivityForResult(intent, 0);
        break;
      }
    }

    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO Auto-generated method stub
    // super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    LOG.d(TAG, ">>> onSharedPreferenceChanged: %s", key);

    if (key.equalsIgnoreCase("pref_url")) {
      this.loadUrl();
    }
  }

}

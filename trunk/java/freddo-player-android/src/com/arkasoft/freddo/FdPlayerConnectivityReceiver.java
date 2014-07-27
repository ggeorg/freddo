package com.arkasoft.freddo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import freddo.dtalk.util.LOG;

public class FdPlayerConnectivityReceiver extends BroadcastReceiver {
  private static final String TAG = LOG.tag(FdPlayerConnectivityReceiver.class);

  @Override
  public void onReceive(Context context, Intent intent) {
    LOG.v(TAG, ">>> onReceive");
    
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean startAtBootime = sharedPreferences.getBoolean(Constants.PREF_AUTO_STARTUP, false);

    if (!MainActivity.mainActivityIsOpen() && !startAtBootime) {
      LOG.d(TAG, "MainActivity is not running; ignore!");
      return;
    }

    Intent service = new Intent(context, FdPlayerService.class);
    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
    if (info.getState().equals(NetworkInfo.State.CONNECTING)) {
      LOG.d(TAG, "NetworkInfo.State.CONNECTING");
      context.startService(service);
    } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
      LOG.d(TAG, "NetworkInfo.State.CONNECTED");
      context.startService(service);
    } else if (info.getState().equals(NetworkInfo.State.DISCONNECTING)) {
      LOG.d(TAG, "NetworkInfo.State.DISCONNECTING");
      context.stopService(service);
    } else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
      LOG.d(TAG, "NetworkInfo.State.DISCONNECTED");
      context.stopService(service);
    }
  }

}

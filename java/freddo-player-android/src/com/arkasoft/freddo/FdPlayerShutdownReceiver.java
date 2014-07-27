package com.arkasoft.freddo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import freddo.dtalk.util.LOG;

public class FdPlayerShutdownReceiver extends BroadcastReceiver {
  private static final String TAG = LOG.tag(FdPlayerShutdownReceiver.class);
  
  @Override
  public void onReceive(Context context, Intent intent) {
    LOG.v(TAG, ">>> onReceive");
    Intent service = new Intent(context, FdPlayerService.class);
    context.stopService(service);
  }
}

package com.arkasoft.freddo;

import org.eclipse.swt.widgets.Shell;

import com.arkasoft.freddo.service.airplay.AirPlayService;
import com.arkasoft.freddo.service.airplay.AirPlayService.Configuration;

import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;

public class FreddoTV extends SWTFdPlayer {
  private static final String TAG = LOG.tag(FreddoTV.class);

  private FreddoTVConfiguration mServiceConfiguration = null;
  private AirPlayService mAirPlayService = null;

  public FreddoTV(Shell shell) {
    super(shell);
  }
  
  @Override
  protected DTalkService.Configuration getConfiguration() {
    if (mServiceConfiguration == null) {
      mServiceConfiguration = new FreddoTVConfiguration();
    }
    return mServiceConfiguration;
  }

  @Override
  protected void onStartup(DTalkService.Configuration conf) {
    LOG.v(TAG, ">>> onStartup");

    // Startup AirPlay server...

    try {
      LOG.i(TAG, "user.dir=%s", System.getProperty("user.dir"));
      mAirPlayService = new AirPlayService((Configuration) conf, null);
      mAirPlayService.startup();
    } catch (Exception e) {
      LOG.e(TAG, "Failed to start AirPlay server", e);
    }
  }

  @Override
  protected void onShutdown() {
    LOG.v(TAG, ">>> onShutdown");

    // Shutdown AirPlay server...
    if (mAirPlayService != null) {
      try {
        mAirPlayService.shutdown();
      } catch (Exception e) {
        LOG.e(TAG, e.getMessage(), e);
      } finally {
        mAirPlayService = null;
      }
    }
  }

}

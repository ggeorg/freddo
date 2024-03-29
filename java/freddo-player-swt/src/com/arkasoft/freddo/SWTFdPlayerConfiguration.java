package com.arkasoft.freddo;

import java.io.IOException;

import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkServiceConfigurationBase;
import freddo.dtalk.util.LOG;

public class SWTFdPlayerConfiguration extends DTalkServiceConfigurationBase {
  private static final String TAG = LOG.tag(SWTFdPlayerConfiguration.class);

  private JmDNS mJmDNS = null;

  @Override
  public JmDNS getJmDNS() {
    if (mJmDNS == null) {
      try {
        mJmDNS = JmDNS.create();
      } catch (IOException e) {
        LOG.e(TAG, "Can't start JmDNS");
      }
    }
    return mJmDNS;
  }

  @Override
  public String getTargetName() {
    return Application.getApplication().getName();
  }

  @Override
  public String getType() {
    return Application.getApplication().getDType();
  }

  @Override
  public String getWebPresenceURL() {
    return null;
  }

  @Override
  public boolean isWebPresence() {
    return false;
  }

}

package com.arkasoft.freddo;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.arkasoft.freddo.nsd.jmdns.NsdManagerImpl;

import freddo.dtalk.DTalkServiceConfigurationBase;
import freddo.dtalk.nsd.NsdManager;

public class FdServiceConfiguration extends DTalkServiceConfigurationBase {

  private final FdPlayerApplication mApplication;

  FdServiceConfiguration(FdPlayerApplication app) {
    super(app.getThreadPool());
    mApplication = app;
  }

  public FdPlayerApplication getApplication() {
    return mApplication;
  }

  @Override
  public NsdManager getNsdManager() {
    return new NsdManagerImpl(mApplication.getJmDNS(), mApplication.getThreadPool());
  }

  @Override
  public String getType() {
    return "Controller/1";
  }

  @Override
  public String getTargetName() {
    return mApplication.getTargetName();
  }

  @Override
  public String getWebPresenceURL() {
    return mApplication.getWebPresenceURL();
  }

  @Override
  public boolean isWebPresenceEnabled() {
    return mApplication.isWebPresenceEnabled();
  }

  @Override
  public int getPort() {
    return 0;//8840;
  }

  @Override
  public InetAddress getInetAddress() {
    try {
      return mApplication.getInetAddress();
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

}

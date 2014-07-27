package com.arkasoft.freddo;

import java.net.NetworkInterface;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;

public class FdServiceConfiguration implements DTalkService.Configuration {

  private final FdPlayer app;

  private byte[] hwAddr = null;

  FdServiceConfiguration(FdPlayer app) {
    this.app = app;
  }

  public FdPlayer getApplication() {
    return app;
  }

  @Override
  public JmDNS getJmDNS() {
    return app.getJmDNS();
  }

  @Override
  public String getDeviceId() {
    return getHardwareAddress("");
  }

  @Override
  public String getType() {
    return "Controller/1";
  }

  @Override
  public String getTargetName() {
    return app.getTargetName();
  }

  @Override
  public String getWebPresenceURL() {
    return app.getWebPresenceURL();
  }

  @Override
  public boolean isWebPresence() {
    return app.isWebPresence();
  }

  @Override
  public ExecutorService getThreadPool() {
    return getApplication().getThreadPool();
  }

  //@Override
  public byte[] getHardwareAddress() {
    if (hwAddr != null && hwAddr.length != 0) {
      return hwAddr;
    }

    try {
      NetworkInterface ni = NetworkInterface.getByInetAddress(getJmDNS().getInterface());
      hwAddr = ni.getHardwareAddress();
    } catch (Exception e) {
      if (LOG.isLoggable(LOG.INFO)) {
        e.printStackTrace();
      }
    }

    if (hwAddr == null || hwAddr.length == 0) {
      Random rand = new Random();
      byte[] mac = new byte[8];
      rand.nextBytes(mac);
      mac[0] = 0x00;
      hwAddr = mac;
    }

    return hwAddr;
  }

  //@Override
  public String getHardwareAddress(String separator) {
    byte[] macAddrs = getHardwareAddress();
    StringBuilder sb = new StringBuilder();
    for (int k = 0; k < macAddrs.length; k++) {
      sb.append(String.format("%02X%s", macAddrs[k], (k < macAddrs.length - 1) ? separator : ""));
    }
    return sb.toString();
  }

  @Override
  public int getPort() {
    return 0;//8840;
  }

}

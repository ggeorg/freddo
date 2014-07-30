/*
 * Copyright 2013-2014 ArkaSoft LLC.
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
package freddo.dtalk;

import java.net.NetworkInterface;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import freddo.dtalk.util.LOG;

/**
 * Base class that for {@link DTalkService.Configuration}.
 * <p>
 * This class implements {@link #getDeviceId()} by using the MAC address. Also
 * implements {@link #getThreadPool()} and {@link #getPort()}.
 */
public abstract class DTalkServiceConfigurationBase implements DTalkService.Configuration {
  
  private final ExecutorService mThreadPool;
  private byte[] hwAddr = null;
  
  protected DTalkServiceConfigurationBase() {
    this(Executors.newCachedThreadPool());
  }
  
  protected DTalkServiceConfigurationBase(ExecutorService threadPool) {
    mThreadPool = threadPool;
  }
  
  @Override
  public final ExecutorService getThreadPool() {
    return mThreadPool;
  }
  
  @Override
  public final String getDeviceId() {
    return getHardwareAddress("");
  }

  @Override
  public final byte[] getHardwareAddress() {
    if (hwAddr != null && hwAddr.length != 0) {
      return hwAddr;
    }

    try {
      NetworkInterface ni = NetworkInterface.getByInetAddress(getInetAddress());
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

  @Override
  public final String getHardwareAddress(String separator) {
    byte[] macAddrs = getHardwareAddress();
    StringBuilder sb = new StringBuilder();
    for (int k = 0; k < macAddrs.length; k++) {
      sb.append(String.format("%02X%s", macAddrs[k], (k < macAddrs.length - 1) ? separator : ""));
    }
    return sb.toString();
  }
  
  @Override
  public int getPort() {
    return 0;
  }
}

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
import java.util.concurrent.TimeUnit;

import freddo.dtalk.util.LOG;

/**
 * Base class that for {@link DTalkService.Configuration}.
 * <p>
 * This class implements {@link #getDeviceId()} by using the MAC address. Also
 * implements {@link #getThreadPool()} and {@link #getPort()}.
 */
public abstract class DTalkServiceConfiguration implements DTalkService.Configuration {
  
  private final ExecutorService mThreadPool;

  private byte[] mHwAddr = null;
  
  protected DTalkServiceConfiguration() {
    this(Executors.newCachedThreadPool());
  }
  
  protected DTalkServiceConfiguration(ExecutorService threadPool) {
  	assert threadPool != null : "ExecutorService is null";
    mThreadPool = threadPool;
  }
  
  @Override
  public final ExecutorService getThreadPool() {
    return mThreadPool;
  }
  
  @Override
  public String getDeviceId() {
    return getHardwareAddress("");
  }

  @Override
  public byte[] getHardwareAddress() {
    if (mHwAddr != null && mHwAddr.length != 0) {
      return mHwAddr;
    }

    try {
      NetworkInterface ni = NetworkInterface.getByInetAddress(getInetAddress());
      mHwAddr = ni.getHardwareAddress();
    } catch (Exception e) {
      if (LOG.isLoggable(LOG.INFO)) {
        e.printStackTrace();
      }
    }

    if (mHwAddr == null || mHwAddr.length == 0) {
      Random rand = new Random();
      byte[] mac = new byte[8];
      rand.nextBytes(mac);
      mac[0] = 0x00;
      mHwAddr = mac;
    }

    return mHwAddr;
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
  
	/**
	 * Shuts down an ExecutorService in two phases, first by calling shutdown to
	 * reject incoming tasks, and then calling shutdownNow, if necessary, to
	 * cancel any lingering tasks.
	 */
	public static void shutdownAndWaitTermination(ExecutorService pool, long timeout) {
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
					System.err.println("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
  
}

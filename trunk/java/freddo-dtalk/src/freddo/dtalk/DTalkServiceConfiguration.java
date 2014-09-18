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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.arkasoft.freddo.jmdns.NetworkTopologyDiscovery;

import freddo.dtalk.util.LOG;

/**
 * Base class that for {@link DTalkService.Configuration}.
 * <p>
 * This class implements {@link #getDeviceId()} by using the MAC address. Also
 * implements {@link #getThreadPool()} and {@link #getPort()}.
 */
public abstract class DTalkServiceConfiguration implements DTalkService.Configuration {
	private static final String TAG = LOG.tag(DTalkServiceConfiguration.class);

	private final ExecutorService mThreadPool;

	private InetSocketAddress mInetSocketAddress = null;

	private byte[] mHwAddr = null;

	protected DTalkServiceConfiguration() {
		this(Executors.newCachedThreadPool());
	}

	protected DTalkServiceConfiguration(ExecutorService threadPool) {
		assert threadPool != null : "ExecutorService is null";
		mThreadPool = threadPool;
	}

	@Override
	public boolean isHosted() {
		return false;
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
	public String getTargetName() {
		return getSocketAddress().getHostName();
	}

	@Override
	public boolean isWebPresenceEnabled() {
		return false;
	}

	@Override
	public String getWebPresenceURL() {
		return null;
	}

	@Override
	public InetSocketAddress getSocketAddress() {
		if (mInetSocketAddress == null) {
			InetAddress addr;
			try {
				addr = InetAddress.getLocalHost();
				if (addr.isLoopbackAddress()) {
					// Find local address that isn't a loopback address
					InetAddress[] addresses = NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses();
					if (addresses.length > 0) {
						addr = addresses[0];
					}
				}
			} catch (UnknownHostException e) {
				LOG.w(TAG, e.getMessage());
				addr = null;
			}
			
			// String aName = addr.getHostName();
			if (addr.isLoopbackAddress()) {
				LOG.w(TAG, "Could not find any address beside the loopback.");
			}

			mInetSocketAddress = new InetSocketAddress(addr, getPort());
		}

		return mInetSocketAddress;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public byte[] getHardwareAddress() {
		if (mHwAddr != null && mHwAddr.length != 0) {
			return mHwAddr;
		}

		try {
			NetworkInterface ni = NetworkInterface.getByInetAddress(getSocketAddress().getAddress());
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

}

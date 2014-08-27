package com.arkasoft.freddo;

import java.io.IOException;
import java.net.InetAddress;

import com.arkasoft.freddo.dtalk.zeroconf.JmDNSZConfManager;
import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkServiceConfiguration;
import freddo.dtalk.zeroconf.ZConfManager;

public class JfxFreddoConfiguration extends DTalkServiceConfiguration {
	
	/** Instance of JmDNS. */
	private final JmDNS mJmDNS;

	/** Instance of ZConfManager. */
	private volatile ZConfManager mZConfManager = null;
	
	public JfxFreddoConfiguration(JmDNS jmDNS) {
		mJmDNS = jmDNS;
	}

	@Override
	public ZConfManager getZConfManager() {
		if (mZConfManager == null) {
			synchronized (ZConfManager.class) {
				if (mZConfManager == null) {
					mZConfManager = new JmDNSZConfManager(mJmDNS, getThreadPool());
				}
			}
		}
		return mZConfManager;
	}

	@Override
	public String getTargetName() {
		return ApplicationDescriptor.getInstance().getName();
	}

	@Override
	public String getType() {
		return ApplicationDescriptor.getInstance().getDType();
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
	public InetAddress getInetAddress() throws IOException {
		return mJmDNS.getInterface();
	}

	@Override
	public boolean runServiceDiscovery() {
		return true;
	}

	@Override
	public boolean registerService() {
		return true;
	}

}

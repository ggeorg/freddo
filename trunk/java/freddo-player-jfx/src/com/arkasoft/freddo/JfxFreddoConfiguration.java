package com.arkasoft.freddo;

import java.io.IOException;
import java.net.InetAddress;

import com.arkasoft.freddo.dtalk.zeroconf.JmDNSZConfManagerImpl;
import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkServiceConfiguration;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfManager;

public class JfxFreddoConfiguration extends DTalkServiceConfiguration {
	private static final String TAG = LOG.tag(JfxFreddoConfiguration.class);
	
	/** Instance of JmDNS. */
	private volatile JmDNS mJmDNS = null;

	/** Instance of ZConfManager. */
	private volatile ZConfManager mZConfManager = null;
	
	private JmDNS getJmDNS() {
		if (mJmDNS == null) {
			synchronized(JmDNS.class) {
				if (mJmDNS == null) {
					try {
						mJmDNS = JmDNS.create();
					} catch (IOException e) {
						LOG.e(TAG, e.getMessage(), e);
					}
				}
			}
		}
		return mJmDNS;
	}

	@Override
	public ZConfManager getZConfManager() {
		if (mZConfManager == null) {
			synchronized (ZConfManager.class) {
				if (mZConfManager == null) {
					mZConfManager = new JmDNSZConfManagerImpl(getJmDNS(), getThreadPool());
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
		return getJmDNS().getInterface();
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

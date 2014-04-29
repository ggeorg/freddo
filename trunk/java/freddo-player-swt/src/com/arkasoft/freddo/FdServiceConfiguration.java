package com.arkasoft.freddo;

import java.io.IOException;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.service.airplay.AirPlayService;

import freddo.dtalk.DTalkServiceConfigurationBase;
import freddo.dtalk.util.LOG;

public class FdServiceConfiguration extends DTalkServiceConfigurationBase implements AirPlayService.Configuration {
	private static final String TAG = LOG.tag(FdServiceConfiguration.class);
	
	private JmDNS jmDNS = null;

	@Override
	public JmDNS getJmDNS() {
		if (jmDNS == null) {
			try {
				jmDNS = JmDNS.create();
			} catch (IOException e) {
				LOG.e(TAG, "Can't start JmDNS");
			}
		}
		return jmDNS;
	}

	@Override
	public String getTargetName() {
		return "Freddo Player";
	}

	@Override
	public String getType() {
		return "Renderer/1; FreddoPlayer/1";
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

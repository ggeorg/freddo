package freddo.dtalk;

import com.arkasoft.freddo.dtalk.zeroconf.JmDNSZConfManager;
import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.zeroconf.ZConfManager;

public abstract class JmDNSDTalkServiceConfiguration extends DTalkServiceConfiguration {

	/** Instance of JmDNS. */
	private final JmDNS mJmDNS;

	/** Instance of ZConfManager. */
	private volatile ZConfManager mZConfManager = null;
	
	public JmDNSDTalkServiceConfiguration(JmDNS jmDNS) {
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
	
}

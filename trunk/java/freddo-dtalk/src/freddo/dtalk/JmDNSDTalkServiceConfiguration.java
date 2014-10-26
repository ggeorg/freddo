package freddo.dtalk;

import java.util.concurrent.ExecutorService;

import com.arkasoft.freddo.dtalk.zeroconf.JmDNSZConfManager;
import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.zeroconf.ZConfManager;

public abstract class JmDNSDTalkServiceConfiguration extends DTalkServiceConfiguration {

	/** Instance of ZConfManager. */
	private volatile ZConfManager mZConfManager = null;
	
	public JmDNSDTalkServiceConfiguration() {
		super();
	}
	
	public JmDNSDTalkServiceConfiguration(ExecutorService threadPool) {
		super(threadPool);
	}

	@Override
	public ZConfManager getZConfManager() {
		if (mZConfManager == null) {
			synchronized (ZConfManager.class) {
				if (mZConfManager == null) {
					mZConfManager = new JmDNSZConfManager(getJmDNS(), getThreadPool());
				}
			}
		}
		return mZConfManager;
	}
	
	public abstract JmDNS getJmDNS();
	
}

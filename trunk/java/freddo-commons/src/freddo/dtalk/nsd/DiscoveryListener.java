package freddo.dtalk.nsd;

/**
 * Interface for callback invocation for service discovery.
 */
public interface DiscoveryListener {

	void onStartDiscoveryFailed(String serviceType, int errorCode);

	void onStopDiscoveryFailed(String serviceType, int errorCode);

	void onDiscoveryStarted(String serviceType);

	void onDiscoveryStopped(String serviceType);

	void onServiceFound(NsdServiceInfo serviceInfo);

	void onServiceLost(NsdServiceInfo serviceInfo);

}

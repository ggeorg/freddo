package freddo.dtalk.zeroconf;

/**
 * Interface for callback invocation for service discovery.
 */
public interface ZConfDiscoveryListener {

	void onServiceFound(ZConfServiceInfo serviceInfo);

	void onServiceLost(ZConfServiceInfo serviceInfo);

}

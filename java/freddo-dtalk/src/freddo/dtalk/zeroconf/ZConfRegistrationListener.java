package freddo.dtalk.zeroconf;

/**
 * Interface for callback invocation for service registration.
 */
public interface ZConfRegistrationListener {

	void onRegistrationFailed(ZConfServiceInfo serviceInfo, int errorCode);

	void onUnregistrationFailed(ZConfServiceInfo serviceInfo, int errorCode);

	void onServiceRegistered(ZConfServiceInfo serviceInfo);

	void onServiceUnregistered(ZConfServiceInfo serviceInfo);

}

package freddo.dtalk.nsd;

/**
 * Interface for callback invocation for service registration.
 */
public interface RegistrationListener {

	void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode);

	void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode);

	void onServiceRegistered(NsdServiceInfo serviceInfo);

	void onServiceUnregistered(NsdServiceInfo serviceInfo);

}

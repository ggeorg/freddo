package freddo.dtalk.nsd;

public interface NsdManager {

  /**
   * Failures are passed with {@link RegistrationListener#onRegistrationFailed},
   * {@link RegistrationListener#onUnregistrationFailed},
   * {@link DiscoveryListener#onStartDiscoveryFailed},
   * {@link DiscoveryListener#onStopDiscoveryFailed} or
   * {@link ResolveListener#onResolveFailed}.
   *
   * Indicates that the operation failed due to an internal error.
   */
  static final int FAILURE_INTERNAL_ERROR = 0;

  /**
   * Indicates that the operation failed because it is already active.
   */
  static final int FAILURE_ALREADY_ACTIVE = 3;

  /**
   * Indicates that the operation failed because the maximum outstanding
   * requests from the applications have reached.
   */
  static final int FAILURE_MAX_LIMIT = 4;

  void discoverServices(String serviceType, DiscoveryListener listener);

  void registerService(NsdServiceInfo serviceInfo, RegistrationListener listener);

  void resolveService(NsdServiceInfo serviceInfo, ResolveListener listener);

  void stopServiceDiscovery(DiscoveryListener listener);

  void unregisterService(RegistrationListener listener);

}

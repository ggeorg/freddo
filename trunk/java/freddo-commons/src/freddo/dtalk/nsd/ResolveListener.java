package freddo.dtalk.nsd;

/**
 * Interface for callback invocation for service resolution.
 */
public interface ResolveListener {

  void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);

  void onServiceResolved(NsdServiceInfo serviceInfo);

}

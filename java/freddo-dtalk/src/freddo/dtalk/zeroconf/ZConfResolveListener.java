package freddo.dtalk.zeroconf;

/**
 * Interface for callback invocation for service resolution.
 */
public interface ZConfResolveListener {

  void onResolveFailed(ZConfServiceInfo serviceInfo, int errorCode);

  void onServiceResolved(ZConfServiceInfo serviceInfo);

}

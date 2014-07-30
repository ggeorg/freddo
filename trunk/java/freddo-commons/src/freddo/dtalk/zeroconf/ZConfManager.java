package freddo.dtalk.zeroconf;

public interface ZConfManager {

  void discoverServices(String serviceType, ZConfDiscoveryListener listener);

  void resolveService(ZConfServiceInfo serviceInfo, ZConfResolveListener listener);

  void stopServiceDiscovery(ZConfDiscoveryListener listener);
  
  void registerService(ZConfServiceInfo serviceInfo, ZConfRegistrationListener listener);

  void unregisterService(ZConfRegistrationListener listener);

}

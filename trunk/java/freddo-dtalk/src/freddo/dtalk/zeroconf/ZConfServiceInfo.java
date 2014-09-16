package freddo.dtalk.zeroconf;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZConfServiceInfo implements Serializable {
  private static final long serialVersionUID = 4939216888818215064L;

  private String mServiceName;

  private String mServiceType;

  private Map<String, String> mTxtRecord;

  private InetAddress mHost;

  private int mPort;

  public ZConfServiceInfo() {
  }

  public ZConfServiceInfo(String serviceName, String serviceType) {
    mServiceName = serviceName;
    mServiceType = serviceType;
  }

  public ZConfServiceInfo(String serviceName, String serviceType, Map<String, String> txtRecords, InetAddress host, int port) {
    mServiceName = serviceName;
    mServiceType = serviceType;
    mTxtRecord = txtRecords;
    mHost = host;
    mPort = port;
  }

  /** Get the service name */
  public String getServiceName() {
    return mServiceName;
  }

  public void setServiceName(String serviceName) {
    this.mServiceName = serviceName;
  }

  /** Get the service type */
  public String getServiceType() {
    return mServiceType;
  }

  public void setServiceType(String serviceType) {
    this.mServiceType = serviceType;
  }

  /** Get a TxtRecord */
  public Map<String, String> getTxtRecord() {
    return Collections.unmodifiableMap(mTxtRecord);
  }

  public void setTxtRecord(Map<String, String> txtRecord) {
    this.mTxtRecord = new ConcurrentHashMap<String, String>(txtRecord);
  }

  /** Get a TxtRecord value for a key */
  public String getTxtRecordValue(String key) {
    return mTxtRecord != null ? mTxtRecord.get(key) : null;
  }

  /** Get the host address. The host address is valid for a resolved service. */
  public InetAddress getHost() {
    return mHost;
  }

  public void setHost(InetAddress host) {
    this.mHost = host;
  }

  /** Get port number. The port number is valid for a resolved service. */
  public int getPort() {
    return mPort;
  }

  public void setPort(int port) {
    this.mPort = port;
  }

  @Override
  public String toString() {
    return "ZConfServiceInfo [ServiceName=" + mServiceName
        + ", ServiceType=" + mServiceType + ", TxtRecord=" + mTxtRecord + ", Host=" + mHost + ", Port=" + mPort + "]";
  }

}

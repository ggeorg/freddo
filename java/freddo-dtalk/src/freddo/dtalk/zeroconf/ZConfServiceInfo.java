package freddo.dtalk.zeroconf;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;

public class ZConfServiceInfo implements Serializable {
  private static final long serialVersionUID = 4939216888818215064L;
  
	/**
	 * Utility method to convert a {@link ZConfServiceInfo} to {@link JSONObject}.
	 */
	public static JSONObject serviceInfoToJSON(ZConfServiceInfo serviceInfo) throws JSONException {
		final JSONObject jsonObj = new JSONObject();

		Set<String> pNames = serviceInfo.getTxtRecord().keySet();
		for (String property : pNames) {
			jsonObj.put(property, serviceInfo.getTxtRecordValue(property));
		}

		jsonObj.put(DTalk.KEY_NAME, serviceInfo.getServiceName());
		jsonObj.put(DTalk.KEY_SERVER, serviceInfo.getHost().getHostAddress());
		jsonObj.put(DTalk.KEY_PORT, serviceInfo.getPort());

		return jsonObj;
	}
	
	/**
	 * Utility method to covert a {@link JSONObject} to {@link ZConfServiceInfo}.
	 */
	public static ZConfServiceInfo jsonToServiceInfo(JSONObject jsonObj) throws JSONException, UnknownHostException {
		final String serviceName = jsonObj.getString(DTalk.KEY_NAME);
		final String host = jsonObj.getString(DTalk.KEY_SERVER);
		final int port = jsonObj.getInt(DTalk.KEY_PORT);
		
		final Map<String, String> txtRecord = new HashMap<String, String>();
		txtRecord.put(DTalk.KEY_PRESENCE_DTALK, "1");
		if (jsonObj.has(DTalk.KEY_PRESENCE_DTYPE)) {
			txtRecord.put(DTalk.KEY_PRESENCE_DTYPE, jsonObj.getString(DTalk.KEY_PRESENCE_DTYPE));
		}
		
		return new ZConfServiceInfo(serviceName, DTalk.SERVICE_TYPE, txtRecord, InetAddress.getByName(host), port);
	}
  
	/**
	 * Utility method to create a new {@link ZConfServiceInfo}.
	 * 
	 * @param serviceName
	 * @param host
	 * @param port
	 * @param dtype
	 * @return
	 */
	public static ZConfServiceInfo newZConfServiceInfo(String serviceName, InetAddress host, int port, String dtype) {
		final Map<String, String> txtRecord = new HashMap<String, String>();
		txtRecord.put(DTalk.KEY_PRESENCE_DTALK, "1");
		txtRecord.put(DTalk.KEY_PRESENCE_DTYPE, dtype);
		return new ZConfServiceInfo(serviceName, DTalk.SERVICE_TYPE, txtRecord, host, port);
	}

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

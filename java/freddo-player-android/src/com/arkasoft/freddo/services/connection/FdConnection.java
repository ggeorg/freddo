package com.arkasoft.freddo.services.connection;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;

public class FdConnection extends FdService {

  public static int NOT_REACHABLE = 0;
  public static int REACHABLE_VIA_CARRIER_DATA_NETWORK = 1;
  public static int REACHABLE_VIA_WIFI_NETWORK = 2;

  public static final String WIFI = "wifi";
  public static final String WIMAX = "wimax";
  // mobile
  public static final String MOBILE = "mobile";
  // 2G network types
  public static final String GSM = "gsm";
  public static final String GPRS = "gprs";
  public static final String EDGE = "edge";
  // 3G network types
  public static final String CDMA = "cdma";
  public static final String UMTS = "umts";
  public static final String HSPA = "hspa";
  public static final String HSUPA = "hsupa";
  public static final String HSDPA = "hsdpa";
  public static final String ONEXRTT = "1xrtt";
  public static final String EHRPD = "ehrpd";
  // 4G network types
  public static final String LTE = "lte";
  public static final String UMB = "umb";
  public static final String HSPA_PLUS = "hspa+";
  // return type
  public static final String TYPE_UNKNOWN = "unknown";
  public static final String TYPE_ETHERNET = "ethernet";
  public static final String TYPE_WIFI = "wifi";
  public static final String TYPE_2G = "2g";
  public static final String TYPE_3G = "3g";
  public static final String TYPE_4G = "4g";
  public static final String TYPE_NONE = "none";

  private boolean registered = false;

  ConnectivityManager sockMan;
  BroadcastReceiver receiver;
  private String lastStatus = "";

  public static final String TYPE = SRV_PREFIX + "Connection";
  public static final String TAG = "Connection";

  protected FdConnection(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
    this.sockMan = (ConnectivityManager) ((Context)getContext()).getSystemService(Context.CONNECTIVITY_SERVICE);

  }

  public void getConnection(JSONObject request) {
    NetworkInfo info = sockMan.getActiveNetworkInfo();
    sendResponse(request, this.getConnectionInfo(info));
  }

  private String getConnectionInfo(NetworkInfo info) {
    String type = TYPE_NONE;
    if (info != null) {
      // If we are not connected to any network set type to none
      if (!info.isConnected()) {
        type = TYPE_NONE;
      }
      else {
        type = getType(info);
      }
    }
    Log.d(TAG, "Connection Type: " + type);
    return type;
  }

  private String getType(NetworkInfo info) {
    if (info != null) {
      String type = info.getTypeName();

      if (type.toLowerCase().equals(WIFI)) {
        return TYPE_WIFI;
      }
      else if (type.toLowerCase().equals(MOBILE)) {
        type = info.getSubtypeName();
        if (type.toLowerCase().equals(GSM) ||
            type.toLowerCase().equals(GPRS) ||
            type.toLowerCase().equals(EDGE)) {
          return TYPE_2G;
        }
        else if (type.toLowerCase().startsWith(CDMA) ||
            type.toLowerCase().equals(UMTS) ||
            type.toLowerCase().equals(ONEXRTT) ||
            type.toLowerCase().equals(EHRPD) ||
            type.toLowerCase().equals(HSUPA) ||
            type.toLowerCase().equals(HSDPA) ||
            type.toLowerCase().equals(HSPA)) {
          return TYPE_3G;
        }
        else if (type.toLowerCase().equals(LTE) ||
            type.toLowerCase().equals(UMB) ||
            type.toLowerCase().equals(HSPA_PLUS)) {
          return TYPE_4G;
        }
      }
    }
    else {
      return TYPE_NONE;
    }
    return TYPE_UNKNOWN;
  }

  @Override
  protected void reset() {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void start() {
    // TODO Auto-generated method stub
    
  }
}

package com.arkasoft.freddo.services.geolocation;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdGeolocation extends FdService {

  public static final String TYPE = SRV_PREFIX + "Geolocation";

  private static final String TAG = LOG.tag(FdGeolocation.class);

  private FdGPSListener gpsListener;
  private FdNetworkListener networkListener;
  private LocationManager locationManager;

  boolean enableHighAccuracy = true;

  protected FdGeolocation(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
    if (locationManager == null) {
      locationManager = (LocationManager) ((Context) getContext()).getSystemService(Context.LOCATION_SERVICE);
    }
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      if (networkListener == null) {
        networkListener = new FdNetworkListener(locationManager, this);
      }
      if (gpsListener == null) {
        gpsListener = new FdGPSListener(locationManager, this);
      }
    }
  }

  @Override
  protected void start() {
    LOG.v(TAG, ">>> start");
    if (enableHighAccuracy) {
      this.gpsListener.start();
    } else {
      this.networkListener.start();
    }
  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");
    if (enableHighAccuracy) {
      this.gpsListener.destroy();
    } else {
      this.networkListener.destroy();
    }
  }

  public JSONObject getLocationJSON(Location loc) {
    JSONObject o = new JSONObject();

    try {
      o.put("latitude", loc.getLatitude());
      o.put("longitude", loc.getLongitude());
      o.put("altitude", (loc.hasAltitude() ? loc.getAltitude() : null));
      o.put("accuracy", loc.getAccuracy());
      o.put("heading", (loc.hasBearing() ? (loc.hasSpeed() ? loc.getBearing() : null) : null));
      o.put("velocity", loc.getSpeed());
      o.put("timestamp", loc.getTime());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return o;
  }

  public void win(Location loc) {
    LOG.v(TAG, ">>> win");

    fireEvent("onstatus", getLocationJSON(loc));
  }

  public void fail(int code, String msg) {
    LOG.v(TAG, ">>> fail");

    JSONObject obj = new JSONObject();
    try {
      obj.put("code", code);
      obj.put("message", msg);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    super.fireEvent("onerror", obj);
  }
}

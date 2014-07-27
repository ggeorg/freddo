package com.arkasoft.freddo.services.geolocation;

import android.location.LocationManager;

public class FdNetworkListener extends FdLocationListener {
  public FdNetworkListener(LocationManager locationManager, FdGeolocation m) {
    super(locationManager, m, "[Fd NetworkListener]");
  }
}

package com.arkasoft.freddo.services.geolocation;

import android.location.LocationManager;

/**
 * This class handles requests for GPS location services.
 * 
 */
public class FdGPSListener extends FdLocationListener {
  public FdGPSListener(LocationManager locationManager, FdGeolocation m) {
    super(locationManager, m, "[Fd GPSListener]");
  }

  /**
   * Start requesting location updates.
   * 
   * @param interval
   */
  @Override
  protected void start() {
    if (!this.running) {
      if (this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
        this.running = true;
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, this);
      } else {
        this.fail(POSITION_UNAVAILABLE, "GPS provider is not available.");
      }
    }
  }
}
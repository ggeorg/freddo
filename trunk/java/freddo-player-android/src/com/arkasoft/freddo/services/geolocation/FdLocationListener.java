package com.arkasoft.freddo.services.geolocation;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class FdLocationListener implements LocationListener {
  public static int PERMISSION_DENIED = 1;
  public static int POSITION_UNAVAILABLE = 2;
  public static int TIMEOUT = 3;

  protected LocationManager locationManager;
  private FdGeolocation owner;
  protected boolean running = false;

  private String TAG = "[Fd Location Listener]";

  public FdLocationListener(LocationManager manager, FdGeolocation broker, String tag) {
    this.locationManager = manager;
    this.owner = broker;
    this.TAG = tag;
  }

  protected void fail(int code, String message) {
    this.owner.fail(code, message);
  }

  private void win(Location loc) {
    this.owner.win(loc);
  }

  /**
   * Location Listener Methods
   */

  /**
   * Called when the provider is disabled by the user.
   * 
   * @param provider
   */
  @Override
  public void onProviderDisabled(String provider) {
    Log.d(TAG, "Location provider '" + provider + "' disabled.");
    this.fail(POSITION_UNAVAILABLE, "GPS provider disabled.");
  }

  /**
   * Called when the provider is enabled by the user.
   * 
   * @param provider
   */
  @Override
  public void onProviderEnabled(String provider) {
    Log.d(TAG, "Location provider " + provider + " has been enabled");
  }

  /**
   * Called when the provider status changes. This method is called when a
   * provider is unable to fetch a location or if the provider has recently
   * become available after a period of unavailability.
   * 
   * @param provider
   * @param status
   * @param extras
   */
  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    Log.d(TAG, "The status of the provider " + provider + " has changed");
    if (status == 0) {
      Log.d(TAG, provider + " is OUT OF SERVICE");
      this.fail(POSITION_UNAVAILABLE, "Provider " +
          provider + " is out of service.");
    }
    else if (status == 1) {
      Log.d(TAG, provider + " is TEMPORARILY_UNAVAILABLE");
    }
    else {
      Log.d(TAG, provider + " is AVAILABLE");
    }
  }

  /**
   * Called when the location has changed.
   * 
   * @param location
   */
  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "The location has been updated!");
    this.win(location);
  }

  // PUBLIC

  /**
   * Destroy listener.
   */
  public void destroy() {
    this.stop();
  }

  // LOCAL

  /**
   * Start requesting location updates.
   * 
   * @param interval
   */
  protected void start() {
    if (!this.running) {
      if (this.locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
        this.running = true;
        this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 10, this);
      } else {
        this.fail(POSITION_UNAVAILABLE, "Network provider is not available.");
      }
    }
  }

  /**
   * Stop receiving location updates.
   */
  private void stop() {
    if (this.running) {
      this.locationManager.removeUpdates(this);
      this.running = false;
    }
  }
}
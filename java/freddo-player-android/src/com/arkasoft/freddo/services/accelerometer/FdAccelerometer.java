package com.arkasoft.freddo.services.accelerometer;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdAccelerometer extends FdService implements SensorEventListener {
  @SuppressWarnings("unused")
  private static final String TAG = LOG.tag(FdAccelerometer.class);

  public static final String TYPE = SRV_PREFIX + "Accelerometer";

  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;

  private float x, y, z; // most recent acceleration values
  private long timestamp; // time of most recent value
  private int status; // status of listener
  private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

  private SensorManager sensorManager; // Sensor manager
  private Sensor mSensor; // Acceleration sensor returned by sensor manager

  private Handler mainHandler = null;
  private Runnable mainRunnable = new Runnable() {
    public void run() {
      FdAccelerometer.this.timeout();
    }
  };

  protected FdAccelerometer(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
    this.x = 0;
    this.y = 0;
    this.z = 0;
    this.timestamp = 0;
    this.setStatus(STOPPED);
    this.sensorManager = (SensorManager) ((Context) getContext()).getSystemService(Context.SENSOR_SERVICE);
  }

  @Override
  protected void reset() {
    this.stop();
  }

  @Override
  protected void start() {
    if (this.status != RUNNING) {
      this.startAccel();
    }
  }

  public void onDestroy() {
    this.stop();
  }

  // --------------------------------------------------------------------------
  // LOCAL METHODS
  // --------------------------------------------------------------------------
  //
  /**
   * Start listening for acceleration sensor.
   * 
   * @return status of listener
   */
  private int startAccel() {
    // If already starting or running, then just return
    if ((this.status == RUNNING) || (this.status == STARTING)) {
      return this.status;
    }

    this.setStatus(STARTING);

    // Get accelerometer from sensor manager
    List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

    // If found, then register as listener
    if ((list != null) && (list.size() > 0)) {
      this.mSensor = list.get(0);
      this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
      this.setStatus(STARTING);
    } else {
      this.setStatus(ERROR_FAILED_TO_START);
      this.fail(ERROR_FAILED_TO_START, "No sensors found to register accelerometer listening to.");
      return this.status;
    }

    // Set a timeout callback on the main thread.
    stopTimeout();
    mainHandler = new Handler(Looper.getMainLooper());
    mainHandler.postDelayed(mainRunnable, 2000);

    return this.status;
  }

  private void stopTimeout() {
    if (mainHandler != null) {
      mainHandler.removeCallbacks(mainRunnable);
    }
  }

  /**
   * Stop listening to acceleration sensor.
   */
  private void stop() {
    stopTimeout();
    if (this.status != STOPPED) {
      this.sensorManager.unregisterListener(this);
    }
    this.setStatus(STOPPED);
    this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
  }

  /**
   * Returns an error if the sensor hasn't started.
   * 
   * Called two seconds after starting the listener.
   */
  private void timeout() {
    if (this.status == STARTING) {
      this.setStatus(ERROR_FAILED_TO_START);
      this.fail(ERROR_FAILED_TO_START, "Accelerometer could not be started.");
    }
  }

  /**
   * Called when the accuracy of the sensor has changed.
   * 
   * @param sensor
   * @param accuracy
   */
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // Only look at accelerometer events
    if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
      return;
    }

    // If not running, then just return
    if (this.status == STOPPED) {
      return;
    }
    this.accuracy = accuracy;
  }

  /**
   * Sensor listener event.
   * 
   * @param SensorEvent event
   */
  public void onSensorChanged(SensorEvent event) {
    // Only look at accelerometer events
    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
      return;
    }

    // If not running, then just return
    if (this.status == STOPPED) {
      return;
    }
    this.setStatus(RUNNING);

    if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

      // Save time that event was received
      this.timestamp = System.currentTimeMillis();
      this.x = event.values[0];
      this.y = event.values[1];
      this.z = event.values[2];

      this.win();
    }
  }

  private void fail(int code, String message) {
    // Error object
    JSONObject errorObj = new JSONObject();
    try {
      errorObj.put("code", code);
      errorObj.put("message", message);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    super.fireEvent("onstatus", errorObj);
  }

  private void win() {
    // Success return object
    super.fireEvent("onstatus", this.getAccelerationJSON());
  }

  private void setStatus(int status) {
    this.status = status;
  }

  private JSONObject getAccelerationJSON() {
    JSONObject r = new JSONObject();
    try {
      r.put("x", this.x);
      r.put("y", this.y);
      r.put("z", this.z);
      r.put("timestamp", this.timestamp);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return r;
  }
}

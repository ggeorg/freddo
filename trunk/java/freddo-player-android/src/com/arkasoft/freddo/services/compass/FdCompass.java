/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arkasoft.freddo.services.compass;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

/**
 * This class listens to the compass sensor and stores the latest heading value.
 * <p>
 * Original code:
 * https://github.com/purplecabbage/cordova-plugin-device-orientation
 */
public class FdCompass extends FdService implements SensorEventListener {
  private static final String TAG = LOG.tag(FdCompass.class);

  public static final String TYPE = SRV_PREFIX + "Compass";

  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;

  private int mStatus; // status of listener
  private float mHeading; // most recent heading value
  private long mTimeStamp; // time of most recent value
  private int mAccuracy; // accuracy of the sensor

  private SensorManager mSensorManager;// Sensor manager
  private Sensor mSensor; // Compass sensor returned by sensor manager

  protected FdCompass(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
    mHeading = 0;
    mTimeStamp = 0;
    setStatus(STOPPED);
    mSensorManager = (SensorManager) ((Context)getContext()).getSystemService(Context.SENSOR_SERVICE);
  }

  @Override
  protected void reset() {
    stop();
  }

  /**
   * GET STATUS request handler.
   * 
   * @param request the request.
   */
  public void getStatus(JSONObject request) {
    sendResponse(request, getStatus());
  }

  /**
   * Get status of compass sensor.
   * 
   * @return status
   */
  private int getStatus() {
    return mStatus;
  }

  private void setStatus(int status) {
    mStatus = status;
  }

  /**
   * Start listening for compass sensor.
   * 
   * @return status of listener
   */
  @Override
  protected void start() {
    // If already starting or running, then just return
    if ((mStatus == RUNNING) || (mStatus == STARTING)) {
      return;// mStatus;
    }

    // Get compass sensor from sensor manager
    @SuppressWarnings("deprecation")
    List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

    // If found, then register as listener
    if (list != null && list.size() > 0) {
      mSensor = list.get(0);
      mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
      setStatus(STARTING);
    }

    // If error, then set status to error & fire onerror event
    else {
      setStatus(ERROR_FAILED_TO_START);
      fireEvent("onerror", mStatus);
    }

    //return mStatus;
  }

  /**
   * Stop listening to compass sensor.
   */
  private void stop() {
    LOG.v(TAG, ">>> stop");
    
    if (mStatus != STOPPED) {
      mSensorManager.unregisterListener(this);
      setStatus(STOPPED);
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    LOG.v(TAG, ">>> onSensorChanged: %s", event);

    // We only care about the orientation as far as it refers to Magnetic North
    float heading = event.values[0];

    // Save heading
    mTimeStamp = System.currentTimeMillis();
    mHeading = heading;
    setStatus(RUNNING);

    // Fire compass ONSTATUS event.
    try {
      fireEvent("onstatus", getCompassHeading());
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    LOG.v(TAG, ">>> onAccuracyChanged: %d", accuracy);

    mAccuracy = accuracy;
  }

  /**
   * Create the CompassHeading JSON object to be returned to JavaScript
   * 
   * @return a compass heading
   */
  private JSONObject getCompassHeading() throws JSONException {
    JSONObject obj = new JSONObject();

    obj.put("magneticHeading", mHeading);
    obj.put("trueHeading", mHeading);
    // Since the magnetic and true heading are always the same our and accuracy
    // is defined as the difference between true and magnetic always return zero
    obj.put("headingAccuracy", mAccuracy);
    obj.put("timestamp", mTimeStamp);

    return obj;
  }

}

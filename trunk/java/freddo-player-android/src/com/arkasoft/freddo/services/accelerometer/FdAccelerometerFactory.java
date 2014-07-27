package com.arkasoft.freddo.services.accelerometer;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdAccelerometerFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdAccelerometer(activity, options);
  }

  @Override
  public String getType() {
    return FdAccelerometer.TYPE;
  }

}

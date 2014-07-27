package com.arkasoft.freddo.services.device;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdDeviceFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdDevice(activity, options);
  }

  @Override
  public String getType() {
    return FdDevice.TYPE;
  }

}

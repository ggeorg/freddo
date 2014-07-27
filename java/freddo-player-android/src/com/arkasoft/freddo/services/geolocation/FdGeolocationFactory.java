package com.arkasoft.freddo.services.geolocation;

import org.json.JSONObject;

import com.arkasoft.freddo.FdActivity;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdGeolocationFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdGeolocation(activity, options);
  }

  @Override
  public String getType() {
    return FdGeolocation.TYPE;
  }

}

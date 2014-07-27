package com.arkasoft.freddo.services.compass;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdCompassFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdCompass(activity, options);
  }

  @Override
  public String getType() {
    return FdCompass.TYPE;
  }

}

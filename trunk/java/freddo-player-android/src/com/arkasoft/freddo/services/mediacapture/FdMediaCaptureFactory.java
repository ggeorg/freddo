package com.arkasoft.freddo.services.mediacapture;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdMediaCaptureFactory implements FdServiceFactory  {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdMediaCapture(activity, options);
  }

  @Override
  public String getType() {
    return FdMediaCapture.TYPE;
  }

}

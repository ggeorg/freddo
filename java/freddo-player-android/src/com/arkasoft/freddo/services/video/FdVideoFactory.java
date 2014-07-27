package com.arkasoft.freddo.services.video;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdVideoFactory implements FdServiceFactory  {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdVideo(activity, options);
  }

  @Override
  public String getType() {
    return FdVideo.TYPE;
  }

}

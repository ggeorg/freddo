package com.arkasoft.freddo.services.video;

import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdVideoFactory implements FdServiceFactory<SWTFdPlayer> {

  @Override
  public FdService<SWTFdPlayer> create(SWTFdPlayer context, JSONObject options) {
    return new FdVideo(context, options);
  }

  @Override
  public String getType() {
    return FdVideo.TYPE;
  }

}

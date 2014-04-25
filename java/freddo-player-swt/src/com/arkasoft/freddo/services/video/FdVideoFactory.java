package com.arkasoft.freddo.services.video;

import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdVideoFactory implements FdServiceFactory<FdPlayer> {

  @Override
  public FdService<FdPlayer> create(FdPlayer context, JSONObject options) {
    return new FdVideo(context, options);
  }

  @Override
  public String getType() {
    return FdVideo.TYPE;
  }

}

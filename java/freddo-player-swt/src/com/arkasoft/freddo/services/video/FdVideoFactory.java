package com.arkasoft.freddo.services.video;

import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayerMain;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdVideoFactory implements FdServiceFactory<SWTFdPlayerMain> {

  @Override
  public FdService<SWTFdPlayerMain> create(SWTFdPlayerMain context, JSONObject options) {
    return new FdVideo(context, options);
  }

  @Override
  public String getType() {
    return FdVideo.TYPE;
  }

}

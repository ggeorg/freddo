package com.arkasoft.freddo.services.presence;

import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdPresenceFactory implements FdServiceFactory<SWTFdPlayer> {

  @Override
  public FdService<SWTFdPlayer> create(SWTFdPlayer context, JSONObject options) {
    return new FdPresence(context, options);
  }

  @Override
  public String getType() {
    return FdPresence.TYPE;
  }

}

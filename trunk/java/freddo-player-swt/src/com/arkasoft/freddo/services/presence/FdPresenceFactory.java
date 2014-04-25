package com.arkasoft.freddo.services.presence;

import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdPresenceFactory implements FdServiceFactory<FdPlayer> {

  @Override
  public FdService<FdPlayer> create(FdPlayer context, JSONObject options) {
    return new FdPresence(context, options);
  }

  @Override
  public String getType() {
    return FdPresence.TYPE;
  }

}

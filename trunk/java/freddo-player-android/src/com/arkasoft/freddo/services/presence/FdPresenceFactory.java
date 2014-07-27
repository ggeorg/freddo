package com.arkasoft.freddo.services.presence;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdPresenceFactory implements FdServiceFactory  {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdPresence(activity, options);
  }

  @Override
  public String getType() {
    return FdPresence.TYPE;
  }

}

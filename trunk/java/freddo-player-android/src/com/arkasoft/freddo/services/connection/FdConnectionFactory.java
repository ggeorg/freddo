package com.arkasoft.freddo.services.connection;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdConnectionFactory implements FdServiceFactory {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdConnection(activity, options);
  }

  @Override
  public String getType() {
    return FdConnection.TYPE;
  }

}

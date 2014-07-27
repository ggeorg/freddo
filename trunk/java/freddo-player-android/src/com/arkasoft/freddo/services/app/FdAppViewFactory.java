package com.arkasoft.freddo.services.app;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdAppViewFactory implements FdServiceFactory  {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdAppView(activity, options);
  }

  @Override
  public String getType() {
    return FdAppView.TYPE;
  }

}

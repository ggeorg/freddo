package com.arkasoft.freddo.services.settings;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdSettingsFactory implements FdServiceFactory  {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdSettings(activity, options);
  }

  @Override
  public String getType() {
    return FdSettings.TYPE;
  }

}

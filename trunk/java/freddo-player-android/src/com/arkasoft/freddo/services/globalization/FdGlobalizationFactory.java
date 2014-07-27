package com.arkasoft.freddo.services.globalization;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdGlobalizationFactory implements FdServiceFactory {

  @Override
  public FdService  create(DTalkServiceContext activity, JSONObject options) {
    return new FdGlobalization(activity, options);
  }

  @Override
  public String getType() {
    return FdGlobalization.TYPE;
  }

}

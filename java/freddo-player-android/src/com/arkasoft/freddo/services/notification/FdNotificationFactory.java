package com.arkasoft.freddo.services.notification;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdNotificationFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdNotification(activity, options);
  }

  @Override
  public String getType() {
    return FdNotification.TYPE;
  }

}

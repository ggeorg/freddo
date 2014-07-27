package com.arkasoft.freddo.services.contacts;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdContactsFactory implements FdServiceFactory {

  @Override
  public FdService create(DTalkServiceContext activity, JSONObject options) {
    return new FdContacts(activity, options);
  }

  @Override
  public String getType() {
    return FdContacts.TYPE;
  }

}

package com.arkasoft.freddo.services.presence;

import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayerMain;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdPresenceFactory implements FdServiceFactory<SWTFdPlayerMain> {

  @Override
  public FdService<SWTFdPlayerMain> create(SWTFdPlayerMain context, JSONObject options) {
    return new FdPresence(context, options);
  }

  @Override
  public String getType() {
    return FdPresence.TYPE;
  }

}

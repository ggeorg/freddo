package com.arkasoft.freddo.services.app;

import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdAppViewFactory implements FdServiceFactory<SWTFdPlayer> {

  @Override
  public FdService<SWTFdPlayer> create(SWTFdPlayer context, JSONObject options) {
    return new FdAppView(context, options);
  }

  @Override
  public String getType() {
    return FdAppView.TYPE;
  }

}

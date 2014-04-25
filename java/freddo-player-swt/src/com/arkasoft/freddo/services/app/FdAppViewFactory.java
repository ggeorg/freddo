package com.arkasoft.freddo.services.app;

import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.services.FdServiceFactory;

public class FdAppViewFactory implements FdServiceFactory<FdPlayer> {

  @Override
  public FdService<FdPlayer> create(FdPlayer context, JSONObject options) {
    return new FdAppView(context, options);
  }

  @Override
  public String getType() {
    return FdAppView.TYPE;
  }

}

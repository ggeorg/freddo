package com.arkasoft.freddo.services;

import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;
import com.arkasoft.freddo.services.app.FdAppViewFactory;
import com.arkasoft.freddo.services.presence.FdPresenceFactory;
import com.arkasoft.freddo.services.video.FdVideoFactory;

import freddo.dtalk.services.FdServiceMgr;

public class SWTFdServiceMgr extends FdServiceMgr<SWTFdPlayer> {

  public SWTFdServiceMgr(SWTFdPlayer context, JSONObject options) {
    super(context, options);
  }

  @Override
  public void start() {
    registerService(new FdAppViewFactory());
    registerService(new FdPresenceFactory());
    registerService(new FdVideoFactory());
  }

  @Override
  protected void runOnUiThread(Runnable r) {
    getContext().runOnUiThread(r);
  }
}

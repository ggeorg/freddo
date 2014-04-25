package com.arkasoft.freddo.services.video;

import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayer;
import com.arkasoft.freddo.services.SWTFdService;

import freddo.dtalk.util.LOG;

public class FdVideo extends SWTFdService {
  private static final String TAG = LOG.tag(FdVideo.class);

  public static final String TYPE = SRV_PREFIX + "Video";

  protected FdVideo(FdPlayer context, JSONObject options) {
    super(context, TYPE, options);
  }

  @Override
  protected void start() {
    LOG.v(TAG, ">>> start");

    try {
      injectJavascript("services/video.js");
    } catch (Throwable t) {
      LOG.e(TAG, "Error in injectJavascript", t);
    }
  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");

    // TODO unregister for dtalk.service.Video events
  }

  public void getSrc(JSONObject request) {
    // implementation in Javascript
  }

  public void setSrc(JSONObject options) {
    // implementation in Javascript
  }
  
  public void setStartPositionPercent(JSONObject options) {
    // implementation in Javascript
  }
  
  public void getInfo(JSONObject request) {
    // implementation in Javascript
  }

  public void doPlay(JSONObject notification) {
    // implementation in Javascript
  }
  
  public void doPause(JSONObject notification) {
    // implementation in Javascript
  }
  
  public void doSeekTo(JSONObject notification) {
    // implementation in Javascript
  }
  
  @Deprecated
  public void doSetRate(JSONObject notification) {
    // implementation in Javascript
  }
  
  public void doStop(JSONObject notification) {
    // implementation in Javascript
  }
}

package com.arkasoft.freddo.services.device;

import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings;
import android.util.Log;

import com.arkasoft.freddo.FdActivity;
import com.arkasoft.freddo.FdPlayer;

import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdDevice extends FdService {
  private static final String TAG = LOG.tag(FdDevice.class);

  public static final String TYPE = SRV_PREFIX + "Device";

  protected FdDevice(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
  }

  public void getInfo(JSONObject request) {
    Log.v(TAG, ">>> getInfo");

    JSONObject response = new JSONObject();
    try {
      response.put("uuid", getUuid());
      response.put("version", getOSVersion());
      response.put("platform", "Android");
      response.put("model", getModel());
      response.put("type", getType());
      sendResponse(request, response);
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage());
      sendErrorResponse(request, DTalkException.INVALID_JSON); // TODO finalize
                                                               // response
                                                               // error...
    }
  }

  public String getType() {
    final FdPlayer player = (FdPlayer) ((FdActivity) getContext()).getApplication();
    return player.getConfiguration().getType();
  }

  public String getUuid() {
    return Settings.Secure.getString(((FdActivity) getContext()).getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);
  }

  public String getOSVersion() {
    return android.os.Build.VERSION.RELEASE;
  }

  public String getModel() {
    return android.os.Build.MODEL;
  }

  @Override
  protected void start() {
    // TODO Auto-generated method stub

  }

  @Override
  protected void reset() {
    // TODO Auto-generated method stub

  }

}

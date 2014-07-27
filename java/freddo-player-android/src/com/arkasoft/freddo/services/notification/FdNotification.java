package com.arkasoft.freddo.services.notification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import com.arkasoft.freddo.FdActivity;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;

public class FdNotification extends FdService {

  public static final String TYPE = SRV_PREFIX + "Notification";

  protected FdNotification(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
  }

  public void doBeep(JSONObject request) {
    try {
      JSONArray array = request.getJSONArray("params");
      beep(array.getInt(0));
      sendResponse(request, true);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void doVibrate(JSONObject request) {
    try {
      JSONObject obj = request.getJSONObject("params");
      vibrate(obj.getInt("miliseconds"));
      sendResponse(request, true);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void beep(long count) {
    Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    Ringtone notification = RingtoneManager.getRingtone(((FdActivity) getContext()).getBaseContext(), ringtone);

    // If phone is not set to silent mode
    if (notification != null) {
      for (long i = 0; i < count; ++i) {
        notification.play();
        long timeout = 5000;
        while (notification.isPlaying() && (timeout > 0)) {
          timeout = timeout - 100;
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }

  /**
   * Vibrates the device for the specified amount of time.
   * 
   * @param time Time to vibrate in ms.
   */
  public void vibrate(long time) {
    // Start the vibration, 0 defaults to half a second.
    if (time == 0) {
      time = 500;
    }
    Vibrator vibrator = (Vibrator) ((FdActivity) getContext()).getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(time);
  }

  @Override
  protected void reset() {
    // TODO Auto-generated method stub

  }

  @Override
  protected void start() {
    // TODO Auto-generated method stub

  }

}

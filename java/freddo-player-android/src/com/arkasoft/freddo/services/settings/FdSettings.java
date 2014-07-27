package com.arkasoft.freddo.services.settings;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.arkasoft.freddo.FdActivity;
import com.arkasoft.freddo.FdPreferencesActivity;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;

public class FdSettings extends FdService {

  public static final String TAG = "Settings";
  public static final String TYPE = SRV_PREFIX + "Settings";

  protected FdSettings(DTalkServiceContext activity, JSONObject options) {
    super(activity, TYPE, options);
  }

  private Object get(String property) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(((FdActivity)getContext()));
    Map<String, ?> values = sharedPrefs.getAll();
    Object value = values.get(property);
    return value;
  }

  private boolean set(JSONObject options) {
    boolean result = false;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(((FdActivity)getContext()));
    Editor editor = sharedPrefs.edit();
    Iterator<?> itr = options.keys();
    while (itr.hasNext()) {
      String key = (String) itr.next();
      try {
        Object value = options.get(key);
        if (value instanceof String) {
          editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
          editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
          editor.putFloat(key, (Float) value);
        } else if (value instanceof Boolean) {
          editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Long) {
          editor.putLong(key, (Long) value);
        }
        editor.commit();
        result = true;
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  public void doSetPreference(JSONObject request) {
    boolean result = set(request.optJSONObject("params"));
    sendResponse(request, result);
  }

  public void doGetPreference(JSONObject request) {
    String key = request.optString("params");
    sendResponse(request, get(key));
  }

  public void doLaunch(JSONObject message) {
    Intent intent = new Intent();
    intent.setClass(((FdActivity)getContext()), FdPreferencesActivity.class);
    ((FdActivity)getContext()).startActivity(intent);
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

package com.arkasoft.freddo.services.presence;

import java.util.Enumeration;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayer;
import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.services.SWTFdService;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkEventListener;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkSubscribeHandle;
import freddo.dtalk.util.LOG;

public class FdPresence extends SWTFdService {
  private static final String TAG = LOG.tag(FdPresence.class);

  public static final String TYPE = SRV_PREFIX + "Presence";

  private DTalkSubscribeHandle mPresenceHandle;

  protected FdPresence(FdPlayer context, JSONObject options) {
    super(context, TYPE, options);
  }

  @Override
  protected void start() {
    LOG.v(TAG, ">>> start");
    
    mPresenceHandle = DTalk.subscribe(DTalk.SERVICE_DTALK_PRESENCE, new DTalkEventListener() {
      @Override
      public void messageSent(String topic, JSONObject event) {
        try {
          String action = event.getString(DTalk.KEY_BODY_ACTION);
          if (DTalk.ACTION_RESOLVED.equals(action)) {
            JSONObject params = event.getJSONObject(DTalk.KEY_BODY_PARAMS);
            LOG.d(TAG, "Presence resolved: %s", params);
            FdPresence.this.fireEvent("resolved", params);
          } else if (DTalk.ACTION_REMOVED.equals(action)) {
            JSONObject params = event.getJSONObject(DTalk.KEY_BODY_PARAMS);
            LOG.d(TAG, "Presence resolved: %s", params);
            FdPresence.this.fireEvent("removed", params);
          }
        } catch (JSONException e) {
          LOG.e(TAG, "JSON error parsing presense event", e);
        }
      }
    });
  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");

    if (mPresenceHandle != null) {
      mPresenceHandle.remove();
      mPresenceHandle = null;
    }
  }
  
  public void getRoster(JSONObject request) {
    LOG.v(TAG, ">>> getRoster");
    
    final JSONArray result = new JSONArray();
    final Map<String, ServiceInfo> map = DTalkService.getInstance().getServiceInfoMap();
    for (String key : map.keySet()) {
      try {
        result.put(serviceInfoToJSON(map.get(key)));
      } catch (JSONException e) {
        LOG.e(TAG, e.getMessage());
      }
    }
    sendResponse(request, result);
  }
  
  protected JSONObject serviceInfoToJSON(ServiceInfo info) throws JSONException {
    final JSONObject jsonObj = new JSONObject();
    final Enumeration<String> pNames = info.getPropertyNames();
    while(pNames.hasMoreElements()) {
      String property = pNames.nextElement();
      jsonObj.put(property, info.getPropertyString(property));
    }
    jsonObj.put(DTalk.KEY_NAME, info.getName());
    jsonObj.put(DTalk.KEY_SERVER, info.getServer());
    jsonObj.put(DTalk.KEY_PORT, info.getPort());
    return jsonObj;
  }

}

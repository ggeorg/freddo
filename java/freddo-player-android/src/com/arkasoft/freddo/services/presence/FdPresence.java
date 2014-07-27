package com.arkasoft.freddo.services.presence;

import java.util.Enumeration;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdPresence extends FdService {
  public static final String TAG = LOG.tag(FdPresence.class);
  
  public static final String TYPE = SRV_PREFIX + "Presence";

  private MessageBusListener<JSONObject> dtalkPresenceListener;

  public FdPresence(DTalkServiceContext activity, JSONObject options) {
    super(activity, TYPE, options);
    
    dtalkPresenceListener  = new MessageBusListener<JSONObject>() {
      @Override
      public void messageSent(String topic, JSONObject message) {
        try {
          if (message.get(MessageEvent.KEY_BODY_ACTION).equals(DTalk.ACTION_RESOLVED)) {
            JSONObject params = message.getJSONObject(DTalk.KEY_BODY_PARAMS);
            LOG.d(TAG, "Resolved: %s", params);
            FdPresence.this.fireEvent("resolved", params);
          } else if (message.get(MessageEvent.KEY_BODY_ACTION).equals(DTalk.ACTION_REMOVED)) {
            JSONObject params = message.getJSONObject(DTalk.KEY_BODY_PARAMS);
            LOG.d(TAG, "Removed: %s", params);
            FdPresence.this.fireEvent("removed", params);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    };

    MessageBus.subscribe(DTalk.SERVICE_DTALK_PRESENCE, dtalkPresenceListener);
  }

  public void getList(JSONObject request) {
    LOG.v(TAG, ">>> getList");
    
    Map<String, ServiceInfo> map = DTalkService.getInstance().getServiceInfoMap();
    JSONArray result = new JSONArray();
    for (String key : map.keySet()) {
      ServiceInfo info = map.get(key);
      try {
        JSONObject r = new JSONObject();
        
        Enumeration<String> pNames = info.getPropertyNames();
        while(pNames.hasMoreElements()) {
          String property = pNames.nextElement();
          r.put(property, info.getPropertyString(property));
        }
        
        r.put(DTalk.KEY_NAME, info.getName());
        r.put(DTalk.KEY_SERVER, info.getServer());
        r.put(DTalk.KEY_PORT, info.getPort());
        
        result.put(r);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    
    LOG.d(TAG, "Presence List: %s", result);
    sendResponse(request, result);
  }
  
  @Override
  protected void start() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");
    
    if (dtalkPresenceListener != null) {
      LOG.v(TAG, "Unregister listener: %s", DTalk.SERVICE_DTALK_PRESENCE);
      MessageBus.unsubscribe(DTalk.SERVICE_DTALK_PRESENCE, dtalkPresenceListener);
      dtalkPresenceListener = null;
    }
  }

}

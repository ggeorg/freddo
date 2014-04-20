/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk.services.clients;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalkException;
import freddo.dtalk.util.LOG;

public class AppView extends Service {
  private static final String TAG = LOG.tag(AppView.class);

  public AppView() {
    super("dtalk.service.AppView");
  }

  public AppView(String target) {
    super("dtalk.service.AppView", target);
  }

  public void getUrl(AsyncCallback<String> callback) throws DTalkException {
    DTalkServiceAdapter.get(this, "url", callback);
  }

  public void setUrl(String url) throws DTalkException {
    DTalkServiceAdapter.set(this, "url", url);
  }
  
  public void reload(boolean clearCache) throws DTalkException {
    DTalkServiceAdapter.invoke(this, "reload", clearCache);
  }

  public void dispatchKeyEvent(String event, int keyCode) throws DTalkException {
    try {
      JSONObject keyEvent = new JSONObject();
      keyEvent.put("keyEvent", event);
      keyEvent.put("keyCode", keyCode);
      DTalkServiceAdapter.invoke(this, "dispatchKeyEvent", keyEvent);
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  public void handleGesture(String gesture) throws DTalkException {
    DTalkServiceAdapter.invoke(this, "handleGestureEvent", gesture);
  }

  public void handleSpokenText(List<String> spokenText) throws DTalkException {
    try {
      JSONArray params = new JSONArray();
      for (int i = 0, n = spokenText.size(); i < n; i++) {
        String txt = spokenText.get(i);
        if (txt == null || "".equals(txt.trim()))
          continue;
        params.put(i, txt.trim().toLowerCase());
      }
      if (params.length() > 0) {
        DTalkServiceAdapter.invoke(this, "handleSpokenTextEvent", params);
      }
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  // --------------------------------------------------------------------------
  // OnLoad Listener
  // --------------------------------------------------------------------------

}

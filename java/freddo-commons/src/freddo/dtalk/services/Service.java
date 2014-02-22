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
package freddo.dtalk.services;

import org.json.JSONException;

public abstract class Service {

  protected final static String SRV_PREFIX = "dtalk.service.";

  private final String name;
  protected final String replyName;

  protected Service(String name) throws JSONException {
    this.name = name;
    this.replyName = '$' + name;

    // Create peer...
    DTalkServiceAdapter.sendStart(this);
  }

  public String getName() {
    return name;
  }

  protected String eventCallbackService(String event) {
    return replyName + "." + event;
  }

  public static void dispose() {
    //DTalkServiceAdapter.sendStop(this);
  }
}

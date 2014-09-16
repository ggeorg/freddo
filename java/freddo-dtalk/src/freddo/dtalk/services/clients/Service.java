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

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.util.AsyncCallback;

public abstract class Service {
  private final String mName;
  private final String mEventCallbackPrefix;

  private String mTarget;

  protected Service(String name) {
    this(name, null);
  }

  protected Service(String name, String target) {
    mName = name;
    mEventCallbackPrefix = String.format("$%s.", name);
    mTarget = target;
  }

  public String getName() {
    return mName;
  }

  public String getTarget() {
    return mTarget;
  }

  public void setTarget(String target) throws DTalkException {
    if ((mTarget == target) || (mTarget != null && mTarget.equals(target))) {
      return;
    }
    if (mTarget != null) {
      stopService();
    }
    mTarget = target;
    if (mTarget != null) {
      startService();
    }
  }

  protected String eventCallbackService(String event) {
    return mEventCallbackPrefix + event;
  }

  public void startService() throws DTalkException {
    try {
      startService(null);
    } catch (RuntimeException e) {
      throw new DTalkException(DTalkException.INTERNAL_ERROR, e.getMessage());
    }
  }

  public void startService(AsyncCallback<Boolean> callback) {
    startService(callback, DTalk.DEFAULT_TIMEOUT);
  }

  public void startService(AsyncCallback<Boolean> callback, long timeout) {
    DTalkServiceAdapter.sendStart(this, callback, timeout);
  }

  public void stopService() throws DTalkException {
    DTalkServiceAdapter.sendStop(this);
  }
}

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
package freddo.dtalk.events;

public class MediaRouterEvent {

  public static enum EventType {
    ADDED, REMOVED, UPDATED
  }

  private final EventType mType;

  private final String mId;
  private final String mName;
  private final String mProtocol;

  public MediaRouterEvent(EventType type, String protocol, String id, String name) {
    mType = type;
    mProtocol = protocol;
    mId = id;
    mName = name;
  }

  public EventType getType() {
    return mType;
  }

  public String getProtocol() {
    return mProtocol;
  }

  public String getId() {
    return mId;
  }

  public String getName() {
    return mName;
  }

  @Override
  public String toString() {
    return "MediaRouterEvent [type=" + mType + ", id=" + mId + ", name=" + mName + ", protocol=" + mProtocol + "]";
  }

}

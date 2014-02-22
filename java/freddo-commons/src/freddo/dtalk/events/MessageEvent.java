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

public interface MessageEvent {
  static String KEY_FROM = "from"; //$NON-NLS-1$
  static String KEY_TO = "to"; //$NON-NLS-1$
  //static String KEY_BODY = "body"; //$NON-NLS-1$
  static String KEY_BODY_VERSION = "dtalk"; //$NON-NLS-1$
  static String KEY_BODY_ID = "id"; //$NON-NLS-1$
  static String KEY_BODY_SERVICE = "service"; //$NON-NLS-1$
  static String KEY_BODY_ACTION = "action"; //$NON-NLS-1$
  static String KEY_BODY_PARAMS = "params"; //$NON-NLS-1$
  static String KEY_BDOY_RESULT = "result"; //$NON-NLS-1$
  static String KEY_BODY_ERROR = "error"; //$NON-NLS-1$
}

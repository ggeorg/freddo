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
  
  static String KEY_ERROR_CODE = "code"; //$NON-NLS-1$
  static String KEY_ERROR_MESSAGE = "message"; //$NON-NLS-1$
  static String KEY_ERROR_DATA = "data"; //$NON-NLS-1$
  
  /** Invalid JSON. */
  static int ERROR_PARSE_ERROR = -32700;
  
  /** The JSON sent is not a valid request object. */
  static int ERROR_INVALID_REQUEST = -32600;
  
  /** The action does not exist / is not available. */
  static int ERROR_ACTION_NOT_FOUND = -32601;
  
  /** Invalid method parameter(s). */
  static int ERROR_INVALID_PARAMS = -326002;
  
  /** Internal DTalk error. */
  static int ERROR_INTERNAL_ERROR = -326003;
  
  /** Server error. */
  static int ERROR_SERVER_ERROR = -32000;
}

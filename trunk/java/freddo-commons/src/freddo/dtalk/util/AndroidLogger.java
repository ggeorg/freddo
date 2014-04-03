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
package freddo.dtalk.util;

import android.util.Log;
import freddo.dtalk.util.LOG.Logger;

public class AndroidLogger implements Logger {

  @Override
  public void d(String tag, String msg) {
    Log.d(tag, msg);
  }

  @Override
  public void d(String tag, String msg, Throwable t) {
    Log.d(tag, msg, t);
  }

  @Override
  public void e(String tag, String msg) {
    Log.e(tag, msg);
  }

  @Override
  public void e(String tag, String msg, Throwable t) {
    Log.e(tag, msg, t);
  }

  @Override
  public void i(String tag, String msg) {
    Log.i(tag, msg);
  }

  @Override
  public void i(String tag, String msg, Throwable t) {
    Log.i(tag, msg, t);
  }

  @Override
  public void v(String tag, String msg) {
    Log.v(tag, msg);
  }

  @Override
  public void v(String tag, String msg, Throwable t) {
    Log.v(tag, msg, t);
  }

  @Override
  public void w(String tag, String msg) {
    Log.w(tag, msg);
  }

  @Override
  public void w(String tag, String msg, Throwable t) {
    Log.w(tag, msg, t);
  }

  @Override
  public void wtf(String tag, String msg) {
    Log.wtf(tag, msg);
  }

  @Override
  public void wtf(String tag, String msg, Throwable t) {
    Log.wtf(tag, msg, t);
  }

  @Override
  public String tag(Class<?> cls) {
    return cls.getSimpleName();
  }

}

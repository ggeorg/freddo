/*
 * Copyright (C) 2013 ArkaSoft, LLC.
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
package com.arkasoft.freddo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import freddo.dtalk.util.LOG;

@SuppressLint("NewApi")
public class FdPreferencesActivity extends PreferenceActivity {
  private final String TAG = LOG.tag(FdPreferencesActivity.class);

  public static final String PREF_APP_VERSION = "app_version";

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    LOG.v(TAG, ">>> onCreate");
    super.onCreate(savedInstanceState);
    
    addPreferencesFromResource(R.xml.preferences);

    //getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    
    // Update APP VERSION...
    //saveStringToPreference(this, PREF_APP_VERSION, getString(R.string.version, getAppVersionName(this)));
    EditTextPreference versionPref = (EditTextPreference) findPreference("app_version");
    versionPref.setTitle(getString(R.string.version, getAppVersionName(this)));
  }

  /**
   * Gets the version of app.
   * 
   * @param context
   * @return
   */
  public static String getAppVersionName(Context context) {
    String versionString = null;
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      versionString = info.versionName;
    } catch (Exception e) {
      // do nothing
    }
    return versionString;
  }

  /**
   * Saves a string value under the provided key in the preference manager. If
   * <code>value</code> is <code>null</code>, then the provided key will be
   * removed from the preferences.
   * 
   * @param context
   * @param key
   * @param value
   */
  public static void saveStringToPreference(Context context, String key, String value) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    if (null == value) {
      // we want to remove
      pref.edit().remove(key).apply();
    } else {
      pref.edit().putString(key, value).apply();
    }
  }
}
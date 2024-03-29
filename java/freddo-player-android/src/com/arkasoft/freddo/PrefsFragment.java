/*
 * Copyright (C) 2013 ArkaSoft, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arkasoft.freddo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import freddo.dtalk.util.LOG;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment {
  private final String TAG = LOG.tag(PrefsFragment.class);

  @Override
  public void onCreate(Bundle savedInstanceState) {
    LOG.v(TAG, ">>> onCreate");
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
  }

}
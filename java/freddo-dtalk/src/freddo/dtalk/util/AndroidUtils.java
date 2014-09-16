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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * A collection of utility methods, all static.
 */
public class AndroidUtils {
  public static final void showErrorAndExit(Context context, String title, String errorString, String btnText) {
    AlertDialog dlg = new AlertDialog.Builder(context).setTitle(title).setMessage(errorString).setPositiveButton(btnText, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
        System.exit(-1);
      }
    }).create();
    dlg.show();
  }
}

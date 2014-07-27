package com.arkasoft.freddo;

import android.content.Intent;

public interface FdActivityResultCallback {

  void onActivityResult(int requestCode, int resultCode, Intent data);

}

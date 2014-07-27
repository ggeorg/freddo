package com.arkasoft.freddo;

import android.content.Intent;

public interface FdPlayerActivityResultCallback {

  void onActivityResult(int requestCode, int resultCode, Intent data);

}

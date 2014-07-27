package com.arkasoft.freddo.services.app;

import android.annotation.TargetApi;
import android.webkit.WebSettings;

@TargetApi(16)
final class Level16Apis {
  static void enableUniversalAccess(final WebSettings settings) {
    settings.setAllowUniversalAccessFromFileURLs(true);
  }
}
package com.arkasoft.freddo.utils;

import java.io.UnsupportedEncodingException;

public class Base64 {
  private static final int FLAGS = android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE;

  public static String encode(String str) {
    return encode(str, null);
  }

  public static String encode(String str, String fallback) {
    try {
      return android.util.Base64.encodeToString(str.getBytes("UTF-8"), FLAGS);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return fallback;
    }
  }

  public static String decode(String str) {
    return decode(str, null);
  }

  public static String decode(String str, String fallback) {
    try {
      return new String(android.util.Base64.decode(str, FLAGS), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return fallback;
    }
  }
}

package com.arkasoft.freddo.utils;

public class ObjectHelper {

  public static boolean equals(Object value, Object newValue) {
    if(value != null) {
      return value.equals(newValue);
    } else {
      return newValue == null;
    }
  }

}

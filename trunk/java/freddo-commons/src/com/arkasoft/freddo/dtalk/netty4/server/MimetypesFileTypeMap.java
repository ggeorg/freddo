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
package com.arkasoft.freddo.dtalk.netty4.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MimetypesFileTypeMap {

  private final static Map<String, String[]> mimeTypes = new ConcurrentHashMap<String, String[]>();

  static {
    String[] mt = new String[] {"application/excel"};
    mimeTypes.put("application/excel", mt);
    mimeTypes.put("xls", mt);
    mimeTypes.put("xlc", mt);
    mimeTypes.put("xll", mt);
    mimeTypes.put("xlm", mt);
    mimeTypes.put("xlw", mt);
    mimeTypes.put("xla", mt);
    mimeTypes.put("xlt", mt);
    mimeTypes.put("xld", mt);
    
    mt = new String[] {"application/powerpoint"};
    mimeTypes.put("application/powerpoint", mt);
    mimeTypes.put("ppz", mt);
    mimeTypes.put("ppt", mt);
    mimeTypes.put("pps", mt);
    mimeTypes.put("pot", mt);
    
    mt = new String[] {"application/msword"};
    mimeTypes.put("application/msword", mt);
    mimeTypes.put("doc", mt);
    
    mt = new String[] {"application/x-compressed-tar"};
    mimeTypes.put("application/x-compressed-tar", mt);
    mimeTypes.put("tgz", mt);
    mimeTypes.put("tar.gz", mt);
    
    mt = new String[] {"application/zip"};
    mimeTypes.put("application/zip", mt);
    mimeTypes.put("zip", mt);
    
    mt = new String[] {"image/jpeg", "image/pjpeg"};
    mimeTypes.put("image/jpeg", mt);
    mimeTypes.put("image/pjpeg", mt);
    mimeTypes.put("jpeg", mt);
    mimeTypes.put("jpg", mt);
    mimeTypes.put("jpe", mt);
    
    mt = new String[] {"image/png"};
    mimeTypes.put("image/png", mt);
    mimeTypes.put("png", mt);
    
    mt = new String[] {"image/gif"};
    mimeTypes.put("image/gif", mt);
    mimeTypes.put("gif", mt);
    
    mt = new String[] {"application/pdf", "application/x-pdf", "image/pdf"};
    mimeTypes.put("application/x-pdf", mt);
    mimeTypes.put("image/pdf", mt);
    mimeTypes.put("pdf", mt);
    
    mt = new String[] {"application/x-bzip", "application/x-bzip2"};
    mimeTypes.put("application/x-bzip", mt);
    mimeTypes.put("application/x-bzip2", mt);
    mimeTypes.put("bz", mt);
    mimeTypes.put("bz2", mt);
    
    mt = new String[] {"application/x-bzip-compressed-tar"};
    mimeTypes.put("application/x-bzip-compressed-tar", mt);
    mimeTypes.put("tar.bz", mt);
    mimeTypes.put("tar.bz2", mt);
    mimeTypes.put("tbz", mt);
    mimeTypes.put("tbz2", mt);
    
    mt = new String[] {"application/x-gzip"};
    mimeTypes.put("application/x-gzip", mt);
    mimeTypes.put("gz", mt);
    
    mt = new String[] {"text/html"};
    mimeTypes.put("text/html", mt);
    mimeTypes.put("html", mt);
    
    mt = new String[] {"text/javascript"};
    mimeTypes.put("text/javascript", mt);
    mimeTypes.put("js", mt);
    
    mt = new String[] {"text/css"};
    mimeTypes.put("text/css", mt);
    mimeTypes.put("css", mt);
    
    mt = new String[] {"video/mp4"};
    mimeTypes.put("video/mp4", mt);
    mimeTypes.put("mp4", mt);
  }

  private static String[] UNKNOWN = new String[] {"application/octet-stream"};

  public static String[] getType(String key) {
    String[] result = mimeTypes.get(key);
    return result != null ? result : UNKNOWN;
  }

  public static String[] getTypeForFileName(String name) {
    String[] result = null;
    for (int pos = name.indexOf('.'); pos != -1;) {
      String key = name.substring(pos + 1);
      result = mimeTypes.get(key);
      if (result != null) {
        break;
      }
      pos = key.indexOf('.');
      name = key;
    }
    return result != null ? result : UNKNOWN;
  }

}

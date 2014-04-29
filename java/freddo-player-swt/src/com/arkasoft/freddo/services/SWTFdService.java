package com.arkasoft.freddo.services;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.browser.Browser;
import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;

import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public abstract class SWTFdService extends FdService<SWTFdPlayer> {
  private static final String TAG = LOG.tag(SWTFdService.class);

  protected SWTFdService(SWTFdPlayer context, String name, JSONObject options) {
    super(context, name, options);
  }

  @Override
  protected void runOnUiThread(Runnable r) {
    getContext().runOnUiThread(r);
  }
  
  protected void injectJavascript(String resource) throws IOException {
    LOG.v(TAG, ">>> injectJavascript: %s", resource);
    
    final Browser browser = getContext().getBrowser();
    if (browser == null) {
      LOG.w(TAG, "Browser is null - failed to inject '%s'", resource);
    }
    
    final String script = readResource(resource);
    if (script != null && script.length() > 0) {
      if (!browser.execute(script)) {
        LOG.d(TAG, "Failed to execute: %s", resource);
      }
    } else {
      LOG.w(TAG, "%s contents are: %s", script);
    }
  }
  
  protected static String readResource(String resource) throws IOException {
    LOG.v(TAG, ">>> readResource: %s", resource);
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream in = null;
    try {
       InputStream resourceAsStream = loader.getResourceAsStream(resource);
       if (resourceAsStream == null) {
          throw new IllegalArgumentException("Could not find resource " + resource);
       }

       in = new BufferedInputStream(resourceAsStream);
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       int b = -1;
       while ((b = in.read()) != -1) {
          out.write(b);
       }
       return new String(out.toByteArray(), "UTF-8");
    } finally {
       try {
          if (in != null) {
             in.close();
          }
       } catch (IOException e) {
         // ignore
       }
    }
 }

}

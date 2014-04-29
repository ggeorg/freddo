package com.arkasoft.freddo;

import java.io.IOException;

import freddo.dtalk.DTalkService;

public abstract class Application {
  
  private static ApplicationDescriptor sApplicationDescriptor = null;

  public static ApplicationDescriptor getApplication() {
    if (sApplicationDescriptor == null) {
      try {
        sApplicationDescriptor = new ApplicationDescriptor();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return sApplicationDescriptor;
  }
  
  protected abstract DTalkService.Configuration getConfiguration();
  
  protected void onStartup(DTalkService.Configuration conf) {
    // startup hook
  }
  
  protected void onShutdown() {
    // shutdown hook
  }

}

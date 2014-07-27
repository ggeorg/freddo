package freddo.dtalk.util;

import java.util.logging.Level;

import freddo.dtalk.util.LOG.Logger;

public class JavaLogger implements Logger {

  @Override
  public void d(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.FINE, msg);
  }

  @Override
  public void d(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.FINE, msg, t); 
  }

  @Override
  public void e(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.SEVERE, msg); 
  }

  @Override
  public void e(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.SEVERE, msg, t);
  }

  @Override
  public void i(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.INFO, msg);
  }

  @Override
  public void i(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.INFO, msg, t);
  }

  @Override
  public void v(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.FINEST, msg);
  }

  @Override
  public void v(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.FINEST, msg, t);
  }

  @Override
  public void w(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.WARNING, msg);
  }

  @Override
  public void w(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.WARNING, msg, t);
  }

  @Override
  public void wtf(String tag, String msg) {
    java.util.logging.Logger.getLogger(tag).log(Level.SEVERE, msg);
    
    // TODO shutdown?
  }

  @Override
  public void wtf(String tag, String msg, Throwable t) {
    java.util.logging.Logger.getLogger(tag).log(Level.SEVERE, msg, t);
    
    // TODO shutdown?
  }

  @Override
  public String tag(Class<?> cls) {
    return cls.getName();
  }

}

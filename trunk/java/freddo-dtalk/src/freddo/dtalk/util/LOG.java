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

/**
 * Logging for lazy people.
 * <p>
 * Log message can be a string or a printf formatted string with arguments. See
 * http://developer.android.com/reference/java/util/Formatter.html
 */
public class LOG {
  private static final String TAG = LOG.class.getName();

  // from android.util.Log
  public static final int VERBOSE = 2;
  public static final int DEBUG = 3;
  public static final int INFO = 4;
  public static final int WARN = 5;
  public static final int ERROR = 6;
  public static final int ASSERT = 7;

  // Current log level
  private static int LOGLEVEL = INFO;

  // Current logger
  private static Logger LOGGER = new Logger() {

    private void log(String fmt, String tag, String msg) {
      System.out.println(String.format(fmt, tag, msg));
    }

    @Override
    public void d(String tag, String msg) {
      log("D: %s - %s", tag, msg);
    }

    @Override
    public void d(String tag, String msg, Throwable t) {
      log("D: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public void e(String tag, String msg) {
      log("E: %s - %s", tag, msg);
    }

    @Override
    public void e(String tag, String msg, Throwable t) {
      log("E: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public void i(String tag, String msg) {
      log("I: %s - %s", tag, msg);
    }

    @Override
    public void i(String tag, String msg, Throwable t) {
      log("I: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public void v(String tag, String msg) {
      log("V: %s - %s", tag, msg);
    }

    @Override
    public void v(String tag, String msg, Throwable t) {
      log("V: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public void w(String tag, String msg) {
      log("W: %s - %s", tag, msg);
    }

    @Override
    public void w(String tag, String msg, Throwable t) {
      log("W: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public void wtf(String tag, String msg) {
      log("***: %s - %s", tag, msg);
    }

    @Override
    public void wtf(String tag, String msg, Throwable t) {
      log("***: %s - %s", tag, msg);
      t.printStackTrace(System.err);
    }

    @Override
    public String tag(Class<?> cls) {
      return cls.getSimpleName();
    }

  };

  /**
   * Set the current log level.
   * 
   * @param logLevel
   */
  public static void setLogLevel(int logLevel) {
    LOGLEVEL = logLevel;
    LOGGER.i(TAG, "Changing log level to " + logLevel);
  }

  /**
   * Set the current {@link Logger} implementation.
   * 
   * @param logger
   */
  public static void setLogger(Logger logger) {
    LOGGER = logger;
    LOGGER.i(TAG, "Changed to: " + LOGGER.getClass().getName());
  }

  /**
   * Determine if log level will be logged
   * 
   * @param logLevel
   * @return
   */
  public static boolean isLoggable(int logLevel) {
    return (logLevel >= LOGLEVEL);
  }

  /**
   * Verbose log message.
   * 
   * @param tag
   * @param s
   */
  public static void v(String tag, String s) {
    if (LOG.VERBOSE >= LOGLEVEL)
      LOGGER.v(tag, s);
  }

  /**
   * Debug log message.
   * 
   * @param tag
   * @param s
   */
  public static void d(String tag, String s) {
    if (LOG.DEBUG >= LOGLEVEL)
      LOGGER.d(tag, s);
  }

  /**
   * Info log message.
   * 
   * @param tag
   * @param s
   */
  public static void i(String tag, String s) {
    if (LOG.INFO >= LOGLEVEL)
      LOGGER.i(tag, s);
  }

  /**
   * Warning log message.
   * 
   * @param tag
   * @param s
   */
  public static void w(String tag, String s) {
    if (LOG.WARN >= LOGLEVEL)
      LOGGER.w(tag, s);
  }

  /**
   * Error log message.
   * 
   * @param tag
   * @param s
   */
  public static void e(String tag, String s) {
    if (LOG.ERROR >= LOGLEVEL)
      LOGGER.e(tag, s);
  }

  /**
   * Verbose log message.
   * 
   * @param tag
   * @param s
   * @param e
   */
  public static void v(String tag, String s, Throwable e) {
    if (LOG.VERBOSE >= LOGLEVEL)
      LOGGER.v(tag, s, e);
  }
  
  /**
   * Verbose log message.
   * 
   * @param tag
   * @param s
   * @param arg
   * @param e
   */
  public static void v(String tag, String s, Object arg, Throwable e) {
    if (LOG.VERBOSE >= LOGLEVEL)
      LOGGER.v(tag, String.format(s, arg), e);
  }

  /**
   * Debug log message.
   * 
   * @param tag
   * @param s
   * @param e
   */
  public static void d(String tag, String s, Throwable e) {
    if (LOG.DEBUG >= LOGLEVEL)
      LOGGER.d(tag, s, e);
  }
  
  /**
   * Debug log message.
   * 
   * @param tag
   * @param s
   * @param arg
   * @param e
   */
  public static void d(String tag, String s, Object arg, Throwable e) {
    if (LOG.DEBUG >= LOGLEVEL)
      LOGGER.d(tag, String.format(s, arg), e);
  }

  /**
   * Info log message.
   * 
   * @param tag
   * @param s
   * @param e
   */
  public static void i(String tag, String s, Throwable e) {
    if (LOG.INFO >= LOGLEVEL)
      LOGGER.i(tag, s, e);
  }
  
  /**
   * Info log message.
   * 
   * @param tag
   * @param s
   * @param arg
   * @param e
   */
  public static void i(String tag, String s, Object arg, Throwable e) {
    if (LOG.INFO >= LOGLEVEL)
      LOGGER.i(tag, String.format(s, arg), e);
  }

  /**
   * Warning log message.
   * 
   * @param tag
   * @param s
   * @param e
   */
  public static void w(String tag, String s, Throwable e) {
    if (LOG.WARN >= LOGLEVEL)
      LOGGER.w(tag, s, e);
  }
  
  /**
   * Warning log message.
   * 
   * @param tag
   * @param s
   * @param arg
   * @param e
   */
  public static void w(String tag, String s, Object arg, Throwable e) {
    if (LOG.WARN >= LOGLEVEL)
      LOGGER.w(tag, String.format(s, arg), e);
  }

  /**
   * Error log message.
   * 
   * @param tag
   * @param s
   * @param e
   */
  public static void e(String tag, String s, Throwable e) {
    if (LOG.ERROR >= LOGLEVEL)
      LOGGER.e(tag, s, e);
  }
  
  /**
   * Error log message.
   * 
   * @param tag
   * @param s
   * @param arg
   * @param e
   */
  public static void e(String tag, String s, Object arg, Throwable e) {
    if (LOG.ERROR >= LOGLEVEL)
      LOGGER.e(tag, String.format(s, arg), e);
  }

  /**
   * Verbose log message with printf formatting.
   * 
   * @param tag
   * @param s
   * @param args
   */
  public static void v(String tag, String s, Object... args) {
    if (LOG.VERBOSE >= LOGLEVEL)
      LOGGER.v(tag, String.format(s, args));
  }

  /**
   * Debug log message with printf formatting.
   * 
   * @param tag
   * @param s
   * @param args
   */
  public static void d(String tag, String s, Object... args) {
    if (LOG.DEBUG >= LOGLEVEL)
      LOGGER.d(tag, String.format(s, args));
  }

  /**
   * Info log message with printf formatting.
   * 
   * @param tag
   * @param s
   * @param args
   */
  public static void i(String tag, String s, Object... args) {
    if (LOG.INFO >= LOGLEVEL)
      LOGGER.i(tag, String.format(s, args));
  }

  /**
   * Warning log message with printf formatting.
   * 
   * @param tag
   * @param s
   * @param args
   */
  public static void w(String tag, String s, Object... args) {
    if (LOG.WARN >= LOGLEVEL)
      LOGGER.w(tag, String.format(s, args));
  }

  /**
   * Error log message with printf formatting.
   * 
   * @param tag
   * @param s
   * @param args
   */
  public static void e(String tag, String s, Object... args) {
    if (LOG.ERROR >= LOGLEVEL)
      LOGGER.e(tag, String.format(s, args));
  }
  
  public static String tag(Class<?> cls) {
    return LOGGER.tag(cls);
  }

  public interface Logger {
    void d(String tag, String msg);

    void d(String tag, String msg, Throwable t);

    void e(String tag, String msg);

    void e(String tag, String msg, Throwable t);

    void i(String tag, String msg);

    void i(String tag, String msg, Throwable t);

    void v(String tag, String msg);

    void v(String tag, String msg, Throwable t);

    void w(String tag, String msg);

    void w(String tag, String msg, Throwable t);

    void wtf(String tag, String msg);

    void wtf(String tag, String msg, Throwable t);
    
    String tag(Class<?> cls);
  }

}

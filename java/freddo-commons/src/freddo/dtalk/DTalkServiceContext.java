package freddo.dtalk;

import java.util.concurrent.ExecutorService;

/**
 * Interface to provide a common application context to all DTalk services on
 * all platforms.
 */
public interface DTalkServiceContext {

  void runOnUiThread(Runnable r);
  
  void assertBackgroundThread();
  
  ExecutorService getThreadPool();

}

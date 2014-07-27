package freddo.dtalk;

/**
 * Interface to provide a common application context to all DTalk services on
 * all platforms.
 */
public interface DTalkServiceContext {

  void runOnUiThread(Runnable r);

}

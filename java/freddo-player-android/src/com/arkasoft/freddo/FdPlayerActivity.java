package com.arkasoft.freddo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.clients.AppView;
import freddo.dtalk.util.AndroidUtils;
import freddo.dtalk.util.LOG;

public abstract class FdPlayerActivity extends Activity implements DTalkServiceContext {
  private static final String TAG = LOG.tag(FdPlayerActivity.class);

  private FdPlayerActivityResultCallback activityResultCallback;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    LOG.v(TAG, ">>> onCreate");
    super.onCreate(savedInstanceState);
  }

  private AppView mAppView = null;

  protected AppView ensureAppView() {
    if (mAppView == null) {
      mAppView = new AppView();
      mAppView.startService(new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable cause) {
          showErrorAndExit("Error", cause.getMessage(), "Exit");
        }

        @Override
        public void onSuccess(Boolean result) {
          if (!result) {
            showErrorAndExit("Error", "Application faild to start", "Exit");
          }
        }
      }, 3333L);
    }
    return mAppView;
  }

  protected void showErrorAndExit(final String title, final String errMessage, final String btnText) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AndroidUtils.showErrorAndExit(FdPlayerActivity.this, title, errMessage, btnText);
      }
    });
  }

  /**
   * Launch an activity for which you would like a result when it finished.
   * <p>
   * NOTE: When this activity exits, your onActivityResult() method will be
   * called.
   * <p>
   * 
   * @param callback The callback object.
   * @param intent The intent to start.
   * @param requestCode The request code that is passed to callback to identify
   *          the activity
   */
  public void startActivityForResult(FdPlayerActivityResultCallback callback, Intent intent, int requestCode) {
    LOG.v(TAG, ">>> startActivityForResult");

    activityResultCallback = callback;

    // TODO multitasking

    // Start activity
    super.startActivityForResult(intent, requestCode);
  }

  /**
   * Called when an activity you launched exits, giving you the
   * {@code requestCode} you started it with, the {@code resultCode} it
   * returned, and any additional data from it.
   * 
   * @param requestCode The request code originally supplied to
   *          startActivityForResult(), allowing you to identify who this result
   *          came from.
   * @param resultCode The integer result code returned by the child activity
   *          through its setResult().
   * @param data An Intent, which can return result data to the caller (various
   *          data can be attached to Intent "extras").
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    LOG.v(TAG, ">>> onActivityResult (requestCode=%d)", requestCode);
    super.onActivityResult(requestCode, resultCode, data);

    FdPlayerActivityResultCallback callback = activityResultCallback;
    if (callback != null) {
      LOG.d(TAG, "We have a callback to send this result to");
      callback.onActivityResult(requestCode, resultCode, data);
    }
  }

  // -------------------------------------------------------------------------
  // SPINNER
  // -------------------------------------------------------------------------

  /**
   * Show the spinner.
   * <p>
   * Must be called from the UI thread.
   * 
   * @param title Title of the dialog
   * @param message The message of the dialog
   */
  public void spinnerStart(final String title, final String message) {
    ProgressBar progressBar = getProgressBar();
    if (progressBar != null) {
      progressBar.setVisibility(View.VISIBLE);
    }
  }

  public void spinnerStart() {
    spinnerStart(null, null);
  }

  /**
   * Stop spinner.
   * <p>
   * Must be called from UI thread.
   */
  public void spinnerStop() {
    ProgressBar progressBar = getProgressBar();
    if (progressBar != null) {
      progressBar.setVisibility(View.INVISIBLE);
    }
  }

  protected abstract ProgressBar getProgressBar();
  
  // --------------------------------------------------------------------------
  // KEY EVENTS
  // --------------------------------------------------------------------------
  
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    String keyEvent;
    switch (event.getAction()) {
      case KeyEvent.ACTION_DOWN:
        keyEvent = "keydown";
        break;
      case KeyEvent.ACTION_UP:
        keyEvent = "keyup";
        break;
      default:
        keyEvent = "undefined";
        break;
    }

    try {
      final int keyCode = event.getKeyCode();
      ensureAppView().dispatchKeyEvent(keyEvent, keyCode);
    } catch (Exception e) {
      LOG.w(TAG, e.getMessage());
    }

    return super.dispatchKeyEvent(event);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

}

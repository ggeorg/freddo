package com.arkasoft.freddo.services.app;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.arkasoft.freddo.FdActivity;
import com.arkasoft.freddo.R;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

@SuppressLint("SetJavaScriptEnabled")
public class FdAppView extends FdService {
  private static final String TAG = LOG.tag(FdAppView.class);

  public static final String TYPE = SRV_PREFIX + "AppView";

  private WebView webView;
  private DTalkJSInterface jsInterface;

  private FrameLayout mVideoViewContainer;
  private View mCustomView = null;
  private WebChromeClient.CustomViewCallback mCustomViewCallback = null;

  private static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
      new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);

  protected FdAppView(DTalkServiceContext activity, JSONObject options) {
    super(activity, TYPE, options);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        LOG.d(TAG, "Performing WebView setup...");

        webView = (WebView) ((Activity) getContext()).findViewById(R.id.appView);
        webView.setVisibility(View.INVISIBLE);
        webView.setBackgroundColor(Color.TRANSPARENT); // ensure transparency
        webView.setWebChromeClient(new FdWebChromeClient());
        webView.setWebViewClient(new FdWebViewClient());
        webView.setInitialScale(0);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.requestFocusFromTouch();

        // webView.setFocusableInTouchMode(false);
        // webView.setFocusable(false);

        // Enable JavaScript
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

        // We don't save any form data in the application
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        // Enable AllowUniversalAccessFromFileURLs
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
          Level16Apis.enableUniversalAccess(settings);
        }

        // Enable database (TODO test)
        String databasePath = ((Activity) getContext()).getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
          settings.setDatabaseEnabled(true);
          settings.setDatabasePath(databasePath);
        }

        settings.setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        settings.setGeolocationEnabled(true);

        // Enable AppCache
        settings.setAppCacheMaxSize(5 * 1048576);
        String pathToCache = ((Activity) getContext()).getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setAppCachePath(pathToCache);
        settings.setAppCacheEnabled(true);

        // Add JavaScript interface only if android version < KitKat
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
          jsInterface = new DTalkJSInterface();
          webView.addJavascriptInterface(jsInterface, "AndroidDTalk");
        }

        // Container for custom view...
        mVideoViewContainer = (FrameLayout) ((Activity) getContext()).findViewById(R.id.videoViewContainer);

        LOG.d(TAG, "WebView setup: done!");
      }
    });
  }

  // AppView interface
  public void setUrl(JSONObject options) {
    LOG.v(TAG, ">>> setUrl");

    String url = getString(options, "url");
    LOG.d(TAG, "Url: %s", url);
    if (url != null) {
      @SuppressWarnings("deprecation")
      String ws = URLEncoder.encode(DTalkService.getInstance().getLocalServiceAddress());
      if (url.contains("?")) {
        url = String.format("%s&ws=%s", url, ws);
      } else {
        url = String.format("%s?ws=%s", url, ws);
      }
      setUrl(url);
    }
  }

  protected void setUrl(final String url) {
    LOG.d(TAG, ">>> setUrl: %s", url);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.clearCache(true);
        webView.loadUrl(url);
      }
    });
  }

  public void setVisibility(JSONObject options) {
    boolean visibility = getBoolean(options, "visibility");
    setVisibility(visibility);
  }

  protected void setVisibility(final boolean visibility) {
    LOG.d(TAG, "Set VISIBILITYL: %s", visibility);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
      }
    });
  }

  public boolean onOverrideUrlLoading(String url) {
    // TODO Auto-generated method stub
    return false;
  }

  protected boolean isUrlWhiteListed(String url) {
    return true;
  }

  protected void onPageStarted(String url) {
    ((FdActivity) getContext()).spinnerStart();
  }

  protected void onPageFinished(String url) {
    ((FdActivity) getContext()).spinnerStop();
  }

  // --------------------------------------------------------------------------
  // ACTIONS
  // --------------------------------------------------------------------------
  public void doDispatchKeyEvent(JSONObject message) {
    int keyCode = message.optInt(MessageEvent.KEY_BODY_PARAMS, -1);
    dispatchKeyEventImpl(keyCode);
  }

  private void dispatchKeyEventImpl(int keyCode) {
    // TODO Auto-generated method stub
  }

  public void doHandleGestureEvent(JSONObject message) {
    JSONObject gesture = message.optJSONObject(MessageEvent.KEY_BODY_PARAMS);
    handleGestureEventImpl(gesture);
  }

  private void handleGestureEventImpl(JSONObject gesture) {
    // TODO Auto-generated method stub
  }

  public void doSpinnerStart(JSONObject message) {
    FdActivity activity = (FdActivity) getContext();
    JSONObject params = message.optJSONObject(MessageEvent.KEY_BODY_PARAMS);
    String spinnerTitle = params.optString("title");
    String spinnerMsg = params.optString("message");
    activity.spinnerStart(spinnerTitle, spinnerMsg);
  }

  public void doSpinnerStop(JSONObject message) {
    FdActivity activity = (FdActivity) getContext();
    activity.spinnerStop();
  }

  public void doReload(JSONObject message) {
    LOG.v(TAG, ">>> doReload");

    final boolean clearCache = message.optBoolean("clearCache", false);
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (clearCache) {
          webView.clearCache(true);
        }
        webView.reload();
      }
    });
  }

  // --------------------------------------------------------------------------
  // FdWebChromeClient implementation
  // --------------------------------------------------------------------------

  /**
   * This class is the {@code WebChromeClient} that implements callbacks for our
   * web view.
   */
  private class FdWebChromeClient extends WebChromeClient {
    private long MAX_QUOTA = 100 * 1024 * 1024;

    @Override
    public void onProgressChanged(WebView view, int progress) {
      // The progress meter will automatically disappear when we reach 100%
      if (progress == 100) {
        ((FdActivity) getContext()).spinnerStop();
      } else {
        ((FdActivity) getContext()).spinnerStart();
      }
    }

    /**
     * Tell the client to display a javascript alert dialog.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
      AlertDialog.Builder dlg = new AlertDialog.Builder(((Activity) getContext()));
      dlg.setMessage(message);
      dlg.setTitle("Alert");
      // Don't let alerts break the back button
      dlg.setCancelable(true);
      dlg.setPositiveButton(android.R.string.ok,
          new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              result.confirm();
            }
          });
      dlg.setOnCancelListener(
          new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              result.cancel();
            }
          });
      dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
        // DO NOTHING
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
            result.confirm();
            return false;
          } else {
            return true;
          }
        }
      });
      dlg.create();
      dlg.show();
      return true;
    }

    /**
     * Tell the client to display a confirm dialog to the user.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
      AlertDialog.Builder dlg = new AlertDialog.Builder(((Activity) getContext()));
      dlg.setMessage(message);
      dlg.setTitle("Confirm");
      dlg.setCancelable(true);
      dlg.setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              result.confirm();
            }
          });
      dlg.setNegativeButton(android.R.string.cancel,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              result.cancel();
            }
          });
      dlg.setOnCancelListener(
          new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              result.cancel();
            }
          });
      dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
        // DO NOTHING
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
            result.cancel();
            return false;
          } else {
            return true;
          }
        }
      });
      dlg.create();
      dlg.show();
      return true;
    }

    /**
     * Tell the client to display a prompt dialog to the user. If the client
     * returns true, WebView will assume that the client will handle the prompt
     * dialog and call the appropriate JsPromptResult method.
     * 
     * Since we are hacking prompts for our own purposes, we should not be using
     * them for this purpose, perhaps we should hack console.log to do this
     * instead!
     * 
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param result
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
      final JsPromptResult res = result;
      AlertDialog.Builder dlg = new AlertDialog.Builder(((Activity) getContext()));
      dlg.setMessage(message);
      final EditText input = new EditText(((Activity) getContext()));
      if (defaultValue != null) {
        input.setText(defaultValue);
      }
      dlg.setView(input);
      dlg.setCancelable(false);
      dlg.setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              String usertext = input.getText().toString();
              res.confirm(usertext);
            }
          });
      dlg.setNegativeButton(android.R.string.cancel,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              res.cancel();
            }
          });
      dlg.create();
      dlg.show();
      return true;
    }

    /**
     * Handle database quota exceeded notification.
     * 
     * @param url
     * @param databaseIdentifier
     * @param currentQuota
     * @param estimatedSize
     * @param totalUsedQuota
     * @param quotaUpdater
     */
    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
      LOG.d(TAG, "onExceededDatabaseQuota estimatedSize: %d  currentQuota: %d  totalUsedQuota: %d", estimatedSize, currentQuota, totalUsedQuota);

      if (estimatedSize < MAX_QUOTA) {
        // increase for 1Mb
        long newQuota = estimatedSize;
        LOG.d(TAG, "calling quotaUpdater.updateQuota newQuota: %d", newQuota);
        quotaUpdater.updateQuota(newQuota);
      } else {
        // Set the quota to whatever it is and force an error
        // TODO: get docs on how to handle this properly
        quotaUpdater.updateQuota(currentQuota);
      }
    }

    @TargetApi(8)
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
      if (consoleMessage.message() != null) {
        LOG.d(TAG, "%s: Line %d : %s", consoleMessage.sourceId(), consoleMessage.lineNumber(), consoleMessage.message());
      }
      return super.onConsoleMessage(consoleMessage);
    }

    /**
     * Instructs the client to show a prompt to ask the user to set the
     * Geolocation permission state for the specified origin.
     * 
     * @param origin
     * @param callback
     */
    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
      super.onGeolocationPermissionsShowPrompt(origin, callback);
      callback.invoke(origin, true, false);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
      LOG.v(TAG, ">>> onShowCustomView");

      // if a view already exists then immediately terminate the new one
      if (mCustomView != null) {
        callback.onCustomViewHidden();
        return;
      }

      // Store the view and its callback for later (to kill it properly)
      mCustomView = view;
      mCustomViewCallback = callback;

      // Add the custom view to its container.
      mVideoViewContainer.addView(view, COVER_SCREEN_GRAVITY_CENTER);
      mVideoViewContainer.invalidate();

      // Hide the content view.
      webView.setVisibility(View.GONE);

      // Finally show the custom view container.
      mVideoViewContainer.setVisibility(View.VISIBLE);
      // mVideoViewContainer.bringToFront();
    }

    @Override
    public void onHideCustomView() {
      LOG.v(TAG, ">>> Hidding Custom View");

      if (mCustomView == null) {
        return;
      }

      // Hide the custom view.
      mCustomView.setVisibility(View.GONE);

      // Remove the custom view from its container.
      // ViewGroup parent = (ViewGroup) webView.getParent();
      mVideoViewContainer.removeView(mCustomView);
      mCustomView = null;
      mCustomViewCallback.onCustomViewHidden();
      mVideoViewContainer.forceLayout();

      // Show the content view.
      webView.setVisibility(View.VISIBLE);
    }

    /**
     * Ask the host application for a custom progress view to show while a
     * <video> is loading.
     * 
     * @return View The progress view.
     */
    // @Override
    // public View getVideoLoadingProgressView() {
    // if (mVideoProgressView == null) {
    // // Create a new Loading view programmatically.
    //
    // // create the linear layout
    // LinearLayout layout = new
    // LinearLayout(this.appView.((Activity)getContext()));
    // layout.setOrientation(LinearLayout.VERTICAL);
    // RelativeLayout.LayoutParams layoutParams = new
    // RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
    // LayoutParams.WRAP_CONTENT);
    // layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    // layout.setLayoutParams(layoutParams);
    // // the proress bar
    // ProgressBar bar = new ProgressBar(this.appView.((Activity)getContext()));
    // LinearLayout.LayoutParams barLayoutParams = new
    // LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
    // LayoutParams.WRAP_CONTENT);
    // barLayoutParams.gravity = Gravity.CENTER;
    // bar.setLayoutParams(barLayoutParams);
    // layout.addView(bar);
    //
    // mVideoProgressView = layout;
    // }
    // return mVideoProgressView;
    // }
  }

  // --------------------------------------------------------------------------
  // FdWebViewClient implementation
  // --------------------------------------------------------------------------

  /**
   * This class is the {@code WebViewClient} that implements callbacks for our
   * web view.
   */
  private class FdWebViewClient extends WebViewClient {
    private boolean doClearHistory = false;

    /** The authorization tokens. */
    private Hashtable<String, AuthenticationToken> authenticationTokens = new Hashtable<String, AuthenticationToken>();

    /**
     * Give the host application a chance to take over the control when a new
     * url is about to be loaded in the current WebView.
     * 
     * @param view The WebView that is initiating the callback.
     * @param url The url to be loaded.
     * @return true to override, false for default behavior
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (FdAppView.this.onOverrideUrlLoading(url)) {
        return true;
      }

      // If dialing phone (tel:5551212)
      if (url.startsWith(WebView.SCHEME_TEL)) {
        try {
          Intent intent = new Intent(Intent.ACTION_DIAL);
          intent.setData(Uri.parse(url));
          ((Activity) getContext()).startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(TAG, "Error dialing " + url + ": " + e.toString());
        }
      }

      // If displaying map (geo:0,0?q=address)
      else if (url.startsWith("geo:")) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          ((Activity) getContext()).startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(TAG, "Error showing map " + url + ": " + e.toString());
        }
      }

      // If sending email (mailto:abc@corp.com)
      else if (url.startsWith(WebView.SCHEME_MAILTO)) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          ((Activity) getContext()).startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(TAG, "Error sending email " + url + ": " + e.toString());
        }
      }

      // If sms:5551212?body=This is the message
      else if (url.startsWith("sms:")) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);

          // Get address
          String address = null;
          int parmIndex = url.indexOf('?');
          if (parmIndex == -1) {
            address = url.substring(4);
          }
          else {
            address = url.substring(4, parmIndex);

            // If body, then set sms body
            Uri uri = Uri.parse(url);
            String query = uri.getQuery();
            if (query != null) {
              if (query.startsWith("body=")) {
                intent.putExtra("sms_body", query.substring(5));
              }
            }
          }
          intent.setData(Uri.parse("sms:" + address));
          intent.putExtra("address", address);
          intent.setType("vnd.android-dir/mms-sms");
          ((Activity) getContext()).startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(TAG, "Error sending sms " + url + ":" + e.toString());
        }
      }

      // Android Market
      else if (url.startsWith("market:")) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          ((Activity) getContext()).startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(TAG, "Error loading Google Play Store: " + url, e);
        }
      }

      // All else
      else {

        // If our app or file:, then load into a new Cordova webview container
        // by starting a new instance of our activity.
        // Our app continues to run. When BACK is pressed, our app is
        // redisplayed.
        if (url.startsWith("file://") || url.startsWith("data:") || isUrlWhiteListed(url)) {
          return false;
        }

        // If not our application, let default viewer handle
        else {
          try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            ((Activity) getContext()).startActivity(intent);
          } catch (android.content.ActivityNotFoundException e) {
            LOG.e(TAG, "Error loading url " + url, e);
          }
        }
      }
      return true;
    }

    /**
     * On received http auth request. The method reacts on all registered
     * authentication tokens. There is one and only one authentication token for
     * any host + realm combination
     * 
     * @param view
     * @param handler
     * @param host
     * @param realm
     */
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

      // Get the authentication token
      AuthenticationToken token = this.getAuthenticationToken(host, realm);
      if (token != null) {
        handler.proceed(token.getUserName(), token.getPassword());
      }
      else {
        // Handle 401 like we'd normally do!
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
      }
    }

    /**
     * Notify the host application that a page has started loading. This method
     * is called once for each main frame load so a page with iframes or
     * framesets will call onPageStarted one time for the main frame. This also
     * means that onPageStarted will not be called when the contents of an
     * embedded frame changes, i.e. clicking a link whose target is an iframe.
     * 
     * @param view The webview initiating the callback.
     * @param url The url of the page.
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      FdAppView.this.onPageStarted(url);
    }

    /**
     * Notify the host application that a page has finished loading. This method
     * is called only for main frame. When onPageFinished() is called, the
     * rendering picture may not be updated yet.
     * 
     * 
     * @param view The webview initiating the callback.
     * @param url The url of the page.
     */
    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      LOG.d(TAG, "onPageFinished(" + url + ")");

      /**
       * Because of a timing issue we need to clear this history in
       * onPageFinished as well as onPageStarted. However we only want to do
       * this if the doClearHistory boolean is set to true. You see when you
       * load a url with a # in it which is common in jQuery applications
       * onPageStared is not called. Clearing the history at that point would
       * break jQuery apps.
       */
      if (this.doClearHistory) {
        view.clearHistory();
        this.doClearHistory = false;
      }

      // Clear timeout flag
      // TODO this.appView.loadUrlTimeout++;

      // Broadcast message that page has loaded
      FdAppView.this.onPageFinished(url);

      // Make app visible after 2 sec in case there was a JS error
      if (webView.getVisibility() == View.INVISIBLE) {
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            // try {
            // Thread.sleep(3000);
            // // getActivity().runOnUiThread(new Runnable() {
            // // public void run() {
            // // appView.postMessage("spinner", "stop");
            // // }
            // // });
            // } catch (InterruptedException e) {
            // }
            setVisibility(true);
          }
        });
        t.start();
      }

      // Shutdown if blank loaded
      // if (url.equals("about:blank")) {
      // fireEvent("exit");
      // }
    }

    /**
     * Report an error to the host application. These errors are unrecoverable
     * (i.e. the main resource is unavailable). The errorCode parameter
     * corresponds to one of the ERROR_* constants.
     * 
     * @param view The WebView that is initiating the callback.
     * @param errorCode The error code corresponding to an ERROR_* value.
     * @param description A String describing the error.
     * @param failingUrl The url that failed to load.
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      LOG.d(TAG, "Error code=%s Description=%s URL=%s", errorCode, description, failingUrl);

      // Clear timeout flag
      // this.appView.loadUrlTimeout++;

      // Handle error
      // JSONObject data = new JSONObject();
      // try {
      // data.put("errorCode", errorCode);
      // data.put("description", description);
      // data.put("url", failingUrl);
      // } catch (JSONException e) {
      // e.printStackTrace();
      // }
      // TODO this.appView.postMessage("onReceivedError", data);

      Toast.makeText(((Activity) getContext()), description, Toast.LENGTH_SHORT).show();
      ((FdActivity) getContext()).spinnerStop();
    }

    /**
     * Notify the host application that an SSL error occurred while loading a
     * resource. The host application must call either handler.cancel() or
     * handler.proceed(). Note that the decision may be retained for use in
     * response to future SSL errors. The default behavior is to cancel the
     * load.
     * 
     * @param view The WebView that is initiating the callback.
     * @param handler An SslErrorHandler object that will handle the user's
     *          response.
     * @param error The SSL error object.
     */
    @TargetApi(8)
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
      final String packageName = ((Activity) getContext()).getPackageName();
      final PackageManager pm = ((Activity) getContext()).getPackageManager();

      ApplicationInfo appInfo;
      try {
        appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
          // debug = true
          handler.proceed();
          return;
        } else {
          // debug = false
          super.onReceivedSslError(view, handler, error);
        }
      } catch (NameNotFoundException e) {
        // When it doubt, lock it out!
        super.onReceivedSslError(view, handler, error);
      }
    }

    /**
     * Sets the authentication token.
     * 
     * @param authenticationToken
     * @param host
     * @param realm
     */
    @SuppressWarnings("unused")
    public void setAuthenticationToken(AuthenticationToken authenticationToken, String host, String realm) {
      if (host == null) {
        host = "";
      }
      if (realm == null) {
        realm = "";
      }
      this.authenticationTokens.put(host.concat(realm), authenticationToken);
    }

    /**
     * Removes the authentication token.
     * 
     * @param host
     * @param realm
     * 
     * @return the authentication token or null if did not exist
     */
    @SuppressWarnings("unused")
    public AuthenticationToken removeAuthenticationToken(String host, String realm) {
      return this.authenticationTokens.remove(host.concat(realm));
    }

    /**
     * Gets the authentication token.
     * 
     * In order it tries: 1- host + realm 2- host 3- realm 4- no host, no realm
     * 
     * @param host
     * @param realm
     * 
     * @return the authentication token
     */
    public AuthenticationToken getAuthenticationToken(String host, String realm) {
      AuthenticationToken token = null;
      token = this.authenticationTokens.get(host.concat(realm));

      if (token == null) {
        // try with just the host
        token = this.authenticationTokens.get(host);

        // Try the realm
        if (token == null) {
          token = this.authenticationTokens.get(realm);
        }

        // if no host found, just query for default
        if (token == null) {
          token = this.authenticationTokens.get("");
        }
      }

      return token;
    }

    /**
     * Clear all authentication tokens.
     */
    @SuppressWarnings("unused")
    public void clearAuthenticationTokens() {
      this.authenticationTokens.clear();
    }

  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset: %s", this);

    synchronized (topicRefCnt) {
      for (String topic : topicRefCnt.keySet()) {
        LOG.d(TAG, "Unregister listener: %s", topic);
        // FIXME: requires API 11
        webView.removeJavascriptInterface("AndroidDTalk");
        MessageBus.unsubscribe(topic, jsInterface);
      }
      topicRefCnt.clear();
    }
  }

  // ---------------------------------------------------------------------
  // WebViewJSInterface
  // ---------------------------------------------------------------------
  private final Map<String, Integer> topicRefCnt = new HashMap<String, Integer>();

  public class DTalkJSInterface implements MessageBusListener<JSONObject> {
    @JavascriptInterface
    public void subscribe(String topic) {
      LOG.v(TAG, ">>> subscribe: %s", topic);
      synchronized (topicRefCnt) {
        if (topicRefCnt.containsKey(topic)) {
          int count = topicRefCnt.get(topic);
          LOG.d(TAG, "topicRefCnt: %d (%s)", count, topic);
          topicRefCnt.put(topic, ++count);
        } else {
          LOG.d(TAG, "Subscribe to MessageBus: %s", topic);
          MessageBus.subscribe(topic, this);
          topicRefCnt.put(topic, 1);
        }
        LOG.d(TAG, "topicRefCount: %s", topicRefCnt.toString());
      }
    }

    @JavascriptInterface
    public void unsubscribe(String topic) {
      LOG.v(TAG, ">>> unsubscribe: %s", topic);
      synchronized (topicRefCnt) {
        if (topicRefCnt.containsKey(topic)) {
          int count = topicRefCnt.get(topic);
          LOG.d(TAG, "topicRefCnt: %d (%s)", count, topic);
          if (--count > 0) {
            topicRefCnt.put(topic, count);
          } else {
            LOG.d(TAG, "UnSubscribe from MessageBus: %s", topic);
            MessageBus.unsubscribe(topic, this);
            topicRefCnt.remove(topic);
          }
          LOG.d(TAG, "topicRefCount: %s", topicRefCnt.toString());
        }
      }
    }

    @JavascriptInterface
    public void send(String message) {
      LOG.v(TAG, ">>> send: %s", message);

      try {
        JSONObject msg = new JSONObject(message);
        String to = msg.optString("to", null);
        if (to == null) {
          MessageBus.sendMessage(new IncomingMessageEvent(msg));
        } else {
          MessageBus.sendMessage(new OutgoingMessageEvent(msg));
        }
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    @Override
    public void messageSent(String topic, JSONObject message) {
      LOG.v(TAG, "messageSent: %s", message.toString());
      final String javascript = buildJavascriptCmd(topic, message);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          webView.loadUrl(javascript);
        }
      });
    }

  }

  static String buildJavascriptCmd(String topic, JSONObject message) {
    StringBuilder sb = new StringBuilder();
    sb.append("javascript:try{DTalk._publish('");
    sb.append(topic);
    sb.append("','");
    // sb.append(Base64.encode(message.toString()));
    sb.append(message.toString().replaceAll("'", "\\\\'"));
    sb.append("');}catch(e){console.log(e);}");
    return sb.toString();
  }

  @Override
  protected void start() {
    // TODO Auto-generated method stub

  }

}

package com.arkasoft.freddo.services.contacts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;

public class FdContacts extends FdService {

  public static final String TAG = "FdContacts";

  public static final String TYPE = SRV_PREFIX + "Contacts";

  private ContactAccessor contactAccessor;

  public static final int UNKNOWN_ERROR = 0;
  public static final int INVALID_ARGUMENT_ERROR = 1;
  public static final int TIMEOUT_ERROR = 2;
  public static final int PENDING_OPERATION_ERROR = 3;
  public static final int IO_ERROR = 4;
  public static final int NOT_SUPPORTED_ERROR = 5;
  public static final int PERMISSION_DENIED_ERROR = 20;

  private final ExecutorService threadPool = Executors.newCachedThreadPool();

  public ExecutorService getThreadPool() {
    return threadPool;
  }

  protected FdContacts(DTalkServiceContext context, JSONObject options) {
    super(context, TYPE, options);
  }

  @Override
  protected void start() {
    /**
     * Check to see if we are on an Android 1.X device.
     */
    if (android.os.Build.VERSION.RELEASE.startsWith("1.")) {
      return;
    }

    /**
     * Only create the contactAccessor after we check the Android version or the
     * program will crash older phones.
     */
    if (this.contactAccessor == null) {
      this.contactAccessor = new ContactAccessorSdk5((Context)getContext());
    }
  }

  public void doSearch(final JSONObject request) {

    if (this.contactAccessor == null) {
      sendResponse(request, "NOT_SUPPORTED_ERROR");
      return;
    }

    final JSONObject params = request.optJSONObject("params");
    final JSONArray fields = params.optJSONArray("fields");
    final JSONObject options = params.optJSONObject("options");
    this.getThreadPool().execute(new Runnable() {
      public void run() {
        JSONArray res = contactAccessor.search(fields, options);
        sendResponse(request, res);
      }
    });
  }

  @Override
  protected void reset() {
    // TODO Auto-generated method stub
    
  }

}

package com.arkasoft.freddo.services.app;

import java.net.URLEncoder;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.json.JSONObject;

import com.arkasoft.freddo.SWTFdPlayer;
import com.arkasoft.freddo.services.SWTFdService;

import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;

public class FdAppView extends SWTFdService {
  private static final String TAG = LOG.tag(FdAppView.class);

  public static final String TYPE = SRV_PREFIX + "AppView";

  protected FdAppView(SWTFdPlayer context, JSONObject options) {
    super(context, TYPE, options);

    Browser b = context.getBrowser();
    b.addLocationListener(new LocationListener() {
      @Override
      public void changed(LocationEvent event) {
        LOG.v(TAG, ">>> LocationListener::changed location=%s", event.location);

      }

      @Override
      public void changing(LocationEvent event) {
        LOG.v(TAG, ">>> LocationListener::changing location=%s", event.location);

      }
    });
    b.addProgressListener(new ProgressListener() {
      @Override
      public void changed(ProgressEvent event) {
        LOG.v(TAG, ">>> ProgressListener::changed: %d/%d", event.current, event.total);
        if (event.total == 0) {
          return;
        }
        double ratio = (double) event.current / (double) event.total;
      }

      @Override
      public void completed(ProgressEvent event) {
        LOG.v(TAG, ">>> ProgressListener::completed");
      }
    });
    b.addStatusTextListener(new StatusTextListener() {
      @Override
      public void changed(StatusTextEvent event) {
        String m = event.text;
        if (m == null) {
          return;
        }
        
        if (m.startsWith("D:")) {
          LOG.d(TAG, m.substring(2));
        } else if (m.startsWith("E:")) {
          LOG.e(TAG, m.substring(2));
        } else if (m.startsWith("W:")) {
          LOG.w(TAG, m.substring(2));
        } else if (m.startsWith("I:")) {
          LOG.i(TAG, m.substring(2));
        }
      }
    });
  }

  @Override
  protected void start() {
    LOG.v(TAG, ">>> start");

  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");

  }

  
  @SuppressWarnings("deprecation")
  public void setUrl(JSONObject options) {
    LOG.v(TAG, ">>> setUrl");
    
    String url = getString(options, "url");
    LOG.d(TAG, "Url: %s", url);
    if (url != null) {
      String ws = URLEncoder.encode(DTalkService.getInstance().getServiceAddressForLocalhost());
      if (url.contains("?")) {
        url = String.format("%s&ws=%s", url, ws);
      } else {
        url = String.format("%s?ws=%s", url, ws);
      }

      LOG.d(TAG, "Url: %s", url);
      getContext().getBrowser().setUrl(url);
    }
  }
}

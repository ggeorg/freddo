package com.arkasoft.freddo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;
import com.arkasoft.freddo.services.SWTFdServiceMgr;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.services.clients.AppView;
import freddo.dtalk.util.LOG;

public class SWTFdPlayerMain extends Application {
  private static final String TAG = LOG.tag(SWTFdPlayerMain.class);

  static {
    LOG.setLogLevel(LOG.VERBOSE);
  }

  private final MessageBusListener<DTalkServiceEvent> mDTalkServiceEventHandler = new MessageBusListener<DTalkServiceEvent>() {
    @Override
    public void messageSent(String topic, DTalkServiceEvent event) {
      onDTalkServiceEvent(event);
    }
  };

  private final Shell mShell;
  private final Browser mBrowser;

  private SWTFdServiceMgr mServiceMgr;

  private ServiceInfo mServiceInfo;

  private AppView mAppView;
  
  private FdServiceConfiguration mServiceConfiguration = null;

  public SWTFdPlayerMain(Shell shell) {
    mShell = shell;
    mShell.setSize(1024, 720);
    mShell.setText(Application.getApplication().getName());
    mShell.setLayout(new FormLayout());
    mShell.addListener(SWT.Close, new Listener() {
      @Override
      public void handleEvent(Event event) {
        LOG.v(TAG, ">>> handleEvent:SWT.Close");
        // mShell.setVisible(false);
        // event.doit = false;
      }
    });

    mShell.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        // TODO Auto-generated method stub
      }
    });

    try {
      mBrowser = new Browser(mShell, SWT.NONE);
      final FormData fd = new FormData();
      fd.top = new FormAttachment(0, 0);
      fd.left = new FormAttachment(0, 0);
      fd.bottom = new FormAttachment(100, 0);
      fd.right = new FormAttachment(100, 0);
      mBrowser.setLayoutData(fd);
    } catch (SWTError e) {
      LOG.e(TAG, "Clould not initialize Browser: %s", e.getMessage());
      throw new RuntimeException(e);
    }

    // Initialize DTalk services
    mServiceMgr = new SWTFdServiceMgr(this, new JSONObject());
    mServiceMgr.start();

    mAppView = new AppView();
    mAppView.startService(new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        LOG.e(TAG, caught.getMessage());
      }

      @Override
      public void onSuccess(Boolean result) {
        if (!result) {
          LOG.e(TAG, "AppView not started...");
        } else {
          if (mServiceInfo != null) {
            // Subscribe to DTalkServiceEvent
            MessageBus.subscribe(DTalkServiceEvent.class.getName(), mDTalkServiceEventHandler);
          } else {
            loadUrl();
          }
        }
      }
    });
  }
  
  @Override
  protected DTalkService.Configuration getConfiguration() {
    if (mServiceConfiguration == null) {
      mServiceConfiguration = new FdServiceConfiguration();
    }
    return mServiceConfiguration;
  }

  protected void onDTalkServiceEvent(DTalkServiceEvent event) {
    LOG.v(TAG, ">>> onDTalkServiceEvent");

    mServiceInfo = event.getServiceInfo();
    if (mServiceInfo != null) {
      loadUrl();
    } else {
      LOG.d(TAG, "++++++++++++++++++++++++++++++++++++ DEAD +++");
    }

    MessageBus.unsubscribe(DTalkServiceEvent.class.getName(), mDTalkServiceEventHandler);
  }

  protected void loadUrl() {
    LOG.v(TAG, ">>> loadUrl");
    loadUrl(getApplication().getIndexFileURI().toString());
  }

  // Has to run outside the UI thread.
  protected void loadUrl(String url) {
    LOG.v(TAG, ">>> loadUrl: %s", url);

    if (mAppView != null) {
      try {
        mAppView.setUrl(url);

        mShell.open();
        mShell.layout();

      } catch (DTalkException e) {
        LOG.e(TAG, e.getMessage());
      }
    }
  }

  public Browser getBrowser() {
    return mBrowser;
  }

  public void runOnUiThread(Runnable r) {
    mShell.getDisplay().asyncExec(r);
  }

  // public boolean isVisible() {
  // return mShell.isVisible();
  // }

  // public void setVisible(boolean visible) {
  // if (visible) {
  // mShell.setVisible(true);
  // } else {
  //
  // }
  // }

  // -----------------------------------------------------------------------

  private static Application sApplication;

  public static void main(String[] args) throws Exception {
    LOG.i(TAG, ">>> Starting: %s", getApplication().getName());

    Display.setAppName(getApplication().getName());
    Display.setAppVersion(getApplication().getId());

    final Display display = new Display();
    final Shell shell = new Shell(display);

    try {
      Class<?> appClassname = Thread.currentThread().getContextClassLoader().loadClass(getApplication().getmApplicationClassName());
      sApplication = (Application) appClassname.getConstructor(Shell.class).newInstance(shell);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    // final Image image = new Image(display, new
    // ImageData(FreddoTV.class.getResourceAsStream("/images/bulb.gif")));
    final Image image = new Image(display, getApplication().getFavicon());
    shell.setImage(image);

    // Create configuration...
    final DTalkService.Configuration conf = sApplication.getConfiguration();
    if (conf.getJmDNS() == null) {
      throw new IllegalStateException("JmDNS not initialized");
    }

    // Initialize DTalkService...
    DTalkService.init(conf);

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG.d(TAG, "Shutdown DTalk service...");
        shutdown();
      }
    });

    // Startup DTalkService...
    conf.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        startup(conf);
      }
    });

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }

    // Run shutdown hook...
    System.exit(0);
  }

  @SuppressWarnings("unused")
  private static Properties loadApplicationDescriptor() throws IOException {
    Properties prop = new Properties();
    prop.load(new FileInputStream(System.getProperty("user.dir") + File.separatorChar + "application.properties"));
    return prop;
  }

  private static void startup(final DTalkService.Configuration conf) {
    LOG.v(TAG, ">>> startup");

    // startup DTalkService...

    try {
      DTalkService.getInstance().startup();
    } catch (Exception e) {
      LOG.e(TAG, "Failed to start DTalkService", e);
    }

    // call startup hook...
    sApplication.onStartup(conf);
  }

  private static void shutdown() {
    LOG.v(TAG, ">>> shutdown");

    // Shutdown DTalkService...
    DTalkService.getInstance().shutdown();

    // call shutdown hook...
    sApplication.onShutdown();
  }
}

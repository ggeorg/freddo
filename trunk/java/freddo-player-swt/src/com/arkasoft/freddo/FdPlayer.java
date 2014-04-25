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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.json.JSONObject;

import com.arkasoft.freddo.jmdns.ServiceInfo;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;
import com.arkasoft.freddo.service.airplay.AirPlayService;
import com.arkasoft.freddo.services.SWTFdServiceMgr;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.services.clients.AppView;
import freddo.dtalk.util.LOG;

public class FdPlayer {
  private static final String TAG = LOG.tag(FdPlayer.class);

  static {
    LOG.setLogLevel(LOG.VERBOSE);
  }

  private final MessageBusListener<DTalkServiceEvent> mDTalkServiceEventHandler =
      new MessageBusListener<DTalkServiceEvent>() {
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

  private FdPlayer(Shell shell, String title) {
    mShell = shell;
    mShell.setSize(1024, 720);
    mShell.setText(title);
    mShell.setLayout(new FormLayout());
    mShell.addListener(SWT.Close, new Listener() {
      @Override
      public void handleEvent(Event event) {
        LOG.v(TAG, ">>> handleEvent:SWT.Close");
        mShell.setVisible(false);
        event.doit = false;
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

        if (sAboutBox != null && sAboutBox.isVisible()) {
          sAboutBox.close();
        }
        
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

//  public boolean isVisible() {
//    return mShell.isVisible();
//  }

//  public void setVisible(boolean visible) {
//    if (visible) {
//      mShell.setVisible(true);
//    } else {
//      
//    }
//  }

  // -----------------------------------------------------------------------

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

  private static FdServiceConfiguration sServiceConfiguration = null;

  public static FdServiceConfiguration getServiceConfiguation() {
    if (sServiceConfiguration == null) {
      sServiceConfiguration = new FdServiceConfiguration();
    }
    return sServiceConfiguration;
  }

  private static AboutBox sAboutBox = null;

  private static AirPlayService mAirPlayService;

  public static void main(String[] args) throws IOException {
    LOG.i(TAG, ">>> Starting: %s", getApplication().getName());

    final FdServiceConfiguration conf = getServiceConfiguation();
    if (conf.getJmDNS() == null) {
      throw new IllegalStateException("JmDNS not initialized");
    }

    // Initialize DTalkService...
    DTalkService.init(conf);

    // Startup DTalkService...
    conf.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        startup(conf);
      }
    });

    // -------------

    final Display display = new Display();
    final Shell shell = new Shell(display);
    final FdPlayer window = new FdPlayer(shell, getApplication().getName());

    // final Image image = new Image(display, new
    // ImageData(FreddoTV.class.getResourceAsStream("/images/bulb.gif")));
    final Image image = new Image(display, getApplication().getFavicon());
    final Tray tray = display.getSystemTray();
    if (tray == null) {
      System.out.println("The system tray is not available");
    } else {
      final TrayItem item = new TrayItem(tray, SWT.NONE);
      item.setToolTipText(getApplication().getName());
      item.addListener(SWT.Show, new Listener() {
        public void handleEvent(Event event) {
          LOG.v(TAG, ">>> handleEvent:SWT.Show");
        }
      });
      item.addListener(SWT.Hide, new Listener() {
        public void handleEvent(Event event) {
          LOG.v(TAG, ">>> handleEvent:SWT.Hide");
        }
      });
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          LOG.v(TAG, ">>> handleEvent:SWT.Selection");
          window.mShell.open();
          window.mShell.layout();

          if (sAboutBox != null && sAboutBox.isVisible()) {
            sAboutBox.close();
          }
        }
      });
      item.addListener(SWT.DefaultSelection, new Listener() {
        public void handleEvent(Event event) {
          LOG.v(TAG, ">>> handleEvent:SWT.DefaultSelection");
        }
      });

      //
      // The Menu
      //

      final Menu menu = new Menu(shell, SWT.POP_UP);

      // About
      MenuItem miAbout = new MenuItem(menu, SWT.PUSH);
      miAbout.setText(String.format("About %s", getApplication().getName()));
      miAbout.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(Event arg0) {
          if (sAboutBox == null) {
            sAboutBox = new AboutBox(shell,
                String.format("About %s", getApplication().getName()),
                getApplication().getAboutFileURI().toString(),
                image);
          }
          sAboutBox.open();
        }
      });

      new MenuItem(menu, SWT.SEPARATOR);

      // Exit
      MenuItem miExit = new MenuItem(menu, SWT.PUSH);
      miExit.setText("Exit");
      miExit.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(Event arg0) {
          item.dispose();
          System.exit(0);
        }
      });

      item.addListener(SWT.MenuDetect, new Listener() {
        public void handleEvent(Event event) {
          LOG.v(TAG, ">>> handleEvent:SWT.MenuDetect");
          menu.setVisible(true);
        }
      });

      item.setImage(image);
      shell.setImage(image);
    }

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG.d(TAG, "Shutdown DTalk service...");
        shutdown();
      }
    });

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }

    System.exit(0);
  }

  @SuppressWarnings("unused")
  private static Properties loadApplicationDescriptor() throws IOException {
    Properties prop = new Properties();
    prop.load(new FileInputStream(System.getProperty("user.dir") + File.separatorChar + "application.properties"));
    return prop;
  }

  private static void startup(final FdServiceConfiguration conf) {
    LOG.v(TAG, ">>> startup");

    // startup DTalk server...

    try {
      DTalkService.getInstance().startup();
    } catch (Exception e) {
      LOG.e(TAG, "Failed to start DTalkService", e);
    }

    // startup AirPlay server...

    try {
      LOG.i(TAG, "user.dir=%s", System.getProperty("user.dir"));
      mAirPlayService = new AirPlayService(conf, null);
      mAirPlayService.startup();
    } catch (Exception e) {
      LOG.e(TAG, "Failed to start AirPlay server", e);
    }
  }

  private static void shutdown() {
    LOG.v(TAG, ">>> shutdown");

    // Shutdown DTalkService...
    DTalkService.getInstance().shutdown();

    // Shutdown AirPlay server...
    if (mAirPlayService != null) {
      try {
        mAirPlayService.shutdown();
      } catch (Exception e) {
        LOG.e(TAG, e.getMessage(), e);
      } finally {
        mAirPlayService = null;
      }
    }
  }
}

package com.arkasoft.freddo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import freddo.dtalk.util.LOG;

public class AboutBox extends Dialog {
  private static final String TAG = LOG.tag(AboutBox.class);

  private final Shell mShell;

  public AboutBox(Shell parent, String title, String url, Image image) {
    super(parent);
    setText(title);
    mShell = createContents(parent, url, image);
  }

  public Image getImage() {
    return mShell.getImage();
  }

  public void open() {
    LOG.v(TAG, ">>> open");

    Shell parent = getParent();

    mShell.pack();

    // shell.layout();

    Rectangle shellBounds = parent.isVisible() ? parent.getBounds() :
        parent.getDisplay().getPrimaryMonitor().getBounds();
    Point dialogSize = mShell.getSize();
    mShell.setLocation(shellBounds.x + (shellBounds.width - dialogSize.x) / 2,
        shellBounds.y + (shellBounds.height - dialogSize.y) / 2);
    
    mShell.open();

    Display display = parent.getDisplay();
    while (!mShell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }

  private Shell createContents(Shell parent, String url, Image image) {
    LOG.v(TAG, ">>> createContents");
    final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    shell.addListener(SWT.Close, new Listener() {
      @Override
      public void handleEvent(Event event) {
        LOG.v(TAG, ">>> handleEvent:SWT.Close");
        shell.setVisible(false);
        event.doit = false;
      }
    });
    shell.setText(getText());
    shell.setImage(image);
    Browser browser = new Browser(shell, SWT.NONE);
    browser.setBounds(0, 0, 450, 300);
    browser.setUrl(url);
    return shell;
  }

  public boolean isVisible() {
    return mShell != null && mShell.isVisible();
  }

  public void close() {
    if (mShell != null) {
      mShell.close();
    }
  }
}

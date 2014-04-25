package com.arkasoft.freddo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.eclipse.swt.graphics.ImageData;

import freddo.dtalk.util.LOG;

public class ApplicationDescriptor {
  private static final String TAG = LOG.tag(ApplicationDescriptor.class);

  private final String mId;
  private final String mName;
  private final String mVendor;
  private final String mCopyright;
  private final File mContentPath;

  private ImageData mFavicon;

  private URI mIndexFileURI;
  private URI mAboutFileURI;

  ApplicationDescriptor() throws IOException {
    final Properties mProperties = loadApplicationProperties();

    mId = mProperties.getProperty("id");
    mName = mProperties.getProperty("name");
    mVendor = mProperties.getProperty("vendor");
    mCopyright = mProperties.getProperty("copyright");

    final StringBuilder sb = new StringBuilder()
        .append(System.getProperty("user.dir"))
        .append(File.separatorChar)
        .append(mProperties.getProperty("content"));
    mContentPath = new File(sb.toString());
    
    if (mContentPath.exists() && mContentPath.isDirectory()) {
      mFavicon = new ImageData(new FileInputStream(new File(mContentPath, "favicon.png")));
      
      mIndexFileURI = new File(mContentPath, "index.html").toURI();
      mAboutFileURI = new File(mContentPath, "about.html").toURI();
    }
    
    // TODO if mFaviconImageData == null use default favicon
  }

  private Properties loadApplicationProperties() throws IOException {
    LOG.v(TAG, ">>> loadApplicationDescriptor");
    final StringBuilder sb = new StringBuilder()
        .append(System.getProperty("user.dir"))
        .append(File.separatorChar)
        .append("application.properties");
    final String appDescriptor = sb.toString();
    LOG.d(TAG, "appDescriptor = '%s'", appDescriptor);
    Properties prop = new Properties();
    prop.load(new FileInputStream(appDescriptor));
    return prop;
  }

  public String getId() {
    return mId;
  }

  public String getName() {
    return mName;
  }

  public String getVendor() {
    return mVendor;
  }

  public String getCopyright() {
    return mCopyright;
  }

  public File getContentPath() {
    return mContentPath;
  }
  
  public ImageData getFavicon() {
    return mFavicon;
  }
  
  public URI getIndexFileURI() {
    return mIndexFileURI;
  }

  public URI getAboutFileURI() {
    return mAboutFileURI;
  }

}

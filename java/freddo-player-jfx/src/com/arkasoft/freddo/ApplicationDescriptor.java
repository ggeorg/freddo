package com.arkasoft.freddo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import freddo.dtalk.util.LOG;

public class ApplicationDescriptor {
	private static final String TAG = LOG.tag(ApplicationDescriptor.class);

	private static volatile ApplicationDescriptor sApplicationDescriptor = null;

	public static ApplicationDescriptor getInstance() {
		if (sApplicationDescriptor == null) {
			synchronized (ApplicationDescriptor.class) {
				if (sApplicationDescriptor == null) {
					try {
						sApplicationDescriptor = new ApplicationDescriptor();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return sApplicationDescriptor;
	}

	private static final String KEY_APP_CLASSNAME = "application.classname";

	private final String mApplicationClassName;

	private final String mId;
	private final String mName;
	private final String mVendor;
	private final String mDType;

	private final String mContentUrl;

	private ApplicationDescriptor() throws IOException {
		final Properties mProperties = loadApplicationProperties();
		mApplicationClassName = mProperties.getProperty(KEY_APP_CLASSNAME);

		mId = mProperties.getProperty("id");
		mName = mProperties.getProperty("name");
		mVendor = mProperties.getProperty("vendor");

		mDType = mProperties.getProperty("dtype");

		mContentUrl = null;
	}

	public String getApplicationClassName() {
		return mApplicationClassName != null ? mApplicationClassName : JfxFreddoApplication.class.getName();
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

	public String getContentUrl() {
		return mContentUrl;
	}

	public String getDType() {
		return mDType;
	}

	private Properties loadApplicationProperties() throws IOException {
		LOG.v(TAG, ">>> loadApplicationProperties");
		final StringBuilder sb = new StringBuilder()
				.append(System.getProperty("user.dir"))
				.append(File.separatorChar)
				.append("application.properties");
		final String appDescriptor = sb.toString();
		LOG.d(TAG, "appDescriptor: '%s'", appDescriptor);
		Properties prop = new Properties();
		prop.load(new FileInputStream(appDescriptor));
		return prop;
	}
}

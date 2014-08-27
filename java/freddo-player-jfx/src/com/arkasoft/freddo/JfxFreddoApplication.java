package com.arkasoft.freddo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import com.arkasoft.freddo.jmdns.JmDNS;
import com.arkasoft.freddo.services.app.JfxFdAppView;
import com.arkasoft.freddo.services.video.JfxFdVideo;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceConfiguration;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdPresence;
import freddo.dtalk.services.FdServiceMgr;
import freddo.dtalk.util.LOG;
import freddo.dtalk.util.Log4JLogger;

public class JfxFreddoApplication extends Application implements DTalkServiceContext {
	private static final String TAG = LOG.tag(JfxFreddoApplication.class);

	static {
		LOG.setLogger(new Log4JLogger());
		LOG.setLogLevel(LOG.VERBOSE);
	}

	private JmDNS mJmDNS = null;
	private FdServiceMgr mServiceMgr = null;

	private WebView mWebView = null;

	public WebView getWebView() {
		return mWebView;
	}

	@Override
	public void init() throws Exception {
		// Create JmDNS and initialize DTalkService.
		mJmDNS = JmDNS.create();
		DTalkService.init(new JfxFreddoConfiguration(mJmDNS));
		DTalkService.getInstance().startup();

		// Create service manager & register services...
		mServiceMgr = new FdServiceMgr(this, null);
		mServiceMgr.registerService(new FdPresence(this, null));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		//
		// Create and register UI based services...
		//

		mWebView = new WebView();
		mServiceMgr.registerService(new JfxFdAppView(this, mWebView, null));
		
		// final MediaView mediaview = new MediaView();
		mServiceMgr.registerService(new JfxFdVideo(this, null));

		//
		// Create UI...
		//

		StackPane stack = new StackPane();
		stack.setStyle("-fx-background-color: black");
		stack.getChildren().addAll(mWebView);

		primaryStage.setScene(new Scene(stack, 1024, 768));
		primaryStage.setTitle(ApplicationDescriptor.getInstance().getName());
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		LOG.v(TAG, ">>> stop");

		// Dispose services
		if (mServiceMgr != null) {
			mServiceMgr.dispose();
			mServiceMgr = null;
		}

		// Shutdown DTalkService
		DTalkService.getInstance().shutdown();

		// Shutdown executor service
		ExecutorService threadPool = DTalkService.getInstance().getConfiguration().getThreadPool();
		DTalkServiceConfiguration.shutdownAndWaitTermination(threadPool, 3333L);

		// Close JmDNS
		mJmDNS.close();

		// XXX
		System.exit(0);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ClassNotFoundException {
		LOG.i(TAG, ">>> Starting: %s", ApplicationDescriptor.getInstance().getName());
		Class<?> applicationClass = JfxFreddoApplication.class.getClassLoader().loadClass(ApplicationDescriptor.getInstance().getApplicationClassName());
		launch((Class<? extends Application>) applicationClass, args);
	}

	@Override
	public void runOnUiThread(Runnable r) {
		Platform.runLater(r);
	}

	@Override
	public void assertBackgroundThread() {
		assert !Platform.isFxApplicationThread() : "Should not run in UI thread.";
	}

	@Override
	public ExecutorService getThreadPool() {
		return DTalkService.getInstance().getConfiguration().getThreadPool();
	}

	public void injectJavascript(String resource) throws IOException {
		LOG.v(TAG, ">>> injectJavascript: %s", resource);

		if (mWebView == null) {
			LOG.w(TAG, "WebView is null - failed to inject '%s'", resource);
		}

		final String script = readResource(resource);
		if (script != null && script.length() > 0) {
			Object obj = mWebView.getEngine().executeScript(script);
			System.out.println("----------------------------------"+obj);
		} else {
			LOG.w(TAG, "%s contents are: %s", script);
		}
	}

	protected static String readResource(String resource) throws IOException {
		LOG.v(TAG, ">>> readResource: %s", resource);

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream in = null;
		try {
			InputStream resourceAsStream = loader.getResourceAsStream(resource);
			if (resourceAsStream == null) {
				throw new IllegalArgumentException("Could not find resource " + resource);
			}

			in = new BufferedInputStream(resourceAsStream);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b = -1;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			return new String(out.toByteArray(), "UTF-8");
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}
	}

}

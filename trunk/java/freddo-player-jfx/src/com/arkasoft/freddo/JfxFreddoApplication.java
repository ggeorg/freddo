package com.arkasoft.freddo;

import java.util.concurrent.ExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdPresence;
import freddo.dtalk.services.FdServiceMgr;
import freddo.dtalk.util.LOG;
import freddo.dtalk.util.Log4jLogger;

public class JfxFreddoApplication extends Application implements DTalkServiceContext {
	private static final String TAG = LOG.tag(JfxFreddoApplication.class);

	static {
		LOG.setLogger(new Log4jLogger());
		LOG.setLogLevel(LOG.VERBOSE);
	}

	private FdServiceMgr mServiceMgr = null;

	@Override
	public void init() throws Exception {
		DTalkService.init(new JfxFreddoConfiguration());
		DTalkService.getInstance().startup();

		// Create service manager & register services
		mServiceMgr  = new FdServiceMgr(this, null);
		mServiceMgr.registerService(new FdPresence(this, null));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		StackPane stack = new StackPane();
		// Pane root = new WebViewPane();
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
		threadPool.shutdownNow();
		
		// XXX Invoke any shutdown hooks registered.
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

}

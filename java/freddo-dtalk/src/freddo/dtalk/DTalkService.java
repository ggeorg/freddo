/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.arkasoft.freddo.dtalk.DTalkConnectionRegistry;
import com.arkasoft.freddo.dtalk.DTalkDiscovery;
import com.arkasoft.freddo.dtalk.DTalkDispatcher;
import com.arkasoft.freddo.dtalk.DTalkServer;
import com.arkasoft.freddo.dtalk.netty4.server.DTalkNettyServerImpl;
import com.arkasoft.freddo.dtalk.netty4.server.DTalkNettyServerInitializer;
import com.arkasoft.freddo.dtalk.netty4.server.NettyConfig;
import com.arkasoft.freddo.dtalk.netty4.server.WebPresenceService;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.events.WebPresenceEvent;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfManager;
import freddo.dtalk.zeroconf.ZConfRegistrationListener;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

public class DTalkService implements Runnable {
	private static final String TAG = LOG.tag(DTalkService.class);

	public static final String LOCAL_CHANNEL_PREFIX = "dtalk-";

	/**
	 * {@code DTalkService} configuration object.
	 */
	public static interface Configuration {

		boolean isHosted();

		ZConfManager getZConfManager();

		String getDeviceId();

		String getType();

		String getTargetName();

		ExecutorService getThreadPool();

		boolean isWebPresenceEnabled();

		String getWebPresenceURL();

		int getPort();

		byte[] getHardwareAddress();

		String getHardwareAddress(String separator);

		InetSocketAddress getSocketAddress();

		boolean runServiceDiscovery();

		boolean registerService();

	}

	/**
	 * The {@code DTalkService} instance.
	 */
	private static volatile DTalkService sInstance = null;

	/**
	 * 
	 * @throws IllgalStateException
	 *           if {@code DTalkService} was not initialized before.
	 */
	public static DTalkService getInstance() {
		if (sInstance == null) {
			throw new IllegalStateException("DTalkService not initialized.");
		}
		return sInstance;
	}

	/**
	 * Create and initialize {@code DTalkService}.
	 * 
	 * @param config
	 *          The {@link DTalkServiceConfiguration}.
	 * @return
	 * 
	 * @throws IllgalStateException
	 *           if {@code DTalkService} is already initialized.
	 */
	public static DTalkService init(Configuration config) {
		LOG.v(TAG, ">>> init");

		if (sInstance == null) {
			synchronized (DTalkService.class) {
				if (sInstance == null) {
					return sInstance = new DTalkService(config);
				}
			}
		}

		throw new IllegalStateException("DTalkService already initialized.");
	}

	// --------------------------------------------------------------------------
	// Event listeners.
	// --------------------------------------------------------------------------

	/**
	 * Listener for {@link WebPresenceEvent}s.
	 */
	private final MessageBusListener<WebPresenceEvent> mWebPresenceEventListener = new MessageBusListener<WebPresenceEvent>() {
		@Override
		public void messageSent(String topic, WebPresenceEvent message) {
			try {
				if (!message.isOpen()) {
					onWebPresenceClosed();
				}
			} catch (Throwable t) {
				LOG.e(TAG, "Unhandled exception in OutgoingMsgEventListener:", t);
			}
		}
	};

	/**
	 * Listener for ZeroConf registration updates.
	 */
	private final ZConfRegistrationListener mZConfRegistrationListener = new ZConfRegistrationListener() {
		@Override
		public void onRegistrationFailed(ZConfServiceInfo serviceInfo, int errorCode) {
			LOG.e(TAG, ">>> onRegistrationFailed: %s (%d)", serviceInfo.getServiceName(), errorCode);
		}

		@Override
		public void onUnregistrationFailed(ZConfServiceInfo serviceInfo, int errorCode) {
			LOG.e(TAG, ">>> onUnregistrationFailed: %s (%d)", serviceInfo.getServiceName(), errorCode);
		}

		@Override
		public void onServiceRegistered(ZConfServiceInfo serviceInfo) {
			LOG.e(TAG, ">>> onServiceRegistered: %s", serviceInfo);
			DTalkService.this.mZConfServiceInfo = serviceInfo;
		}

		@Override
		public void onServiceUnregistered(ZConfServiceInfo serviceInfo) {
			LOG.e(TAG, ">>> onRegistrationFailed: %s", serviceInfo);
			DTalkService.this.mZConfServiceInfo = null;
		}
	};

	// --------------------------------------------------------------------------

	/** DTalkService.Configuration instance to use. */
	private final Configuration mConfiguration;

	/** The embedded WebSocket server to use. */
	private final DTalkServer mDTalkServer;
	
	/** The discovery service to use. */
	private final DTalkDiscovery mServiceDiscovery;

	/** The local Zeroconf service info. */
	private ZConfServiceInfo mZConfServiceInfo = null;

	/** Runtime state of DTalkService instance. */
	private boolean mStarted = false;

	/**
	 * {@code DTalkService} constructor.
	 * 
	 * @param configuration
	 */
	private DTalkService(Configuration configuration) {
		assert configuration != null : "configuration is null";

		// Store configuration object.
		mConfiguration = configuration;

		// Create DTalkServer.
		if (!mConfiguration.isHosted()) {
			NettyConfig nettyConfig = new NettyConfig(mConfiguration.getSocketAddress());
			DTalkNettyServerInitializer initializer = new DTalkNettyServerInitializer();
			mDTalkServer = new DTalkNettyServerImpl(nettyConfig, initializer);
		} else {
			mDTalkServer = null;
		}

		// Create DTalkDiscoveryService.
		mServiceDiscovery = mConfiguration.runServiceDiscovery() ? new DTalkDiscovery() : null;

		// Start dispatcher...
		DTalkDispatcher.start();
	}

	public Configuration getConfiguration() {
		return mConfiguration;
	}

	public ZConfServiceInfo getLocalServiceInfo() {
		assert mStarted == true : "DTalkService not started.";
		return mZConfServiceInfo;
	}

	// In case there is no discovery running this map is used.
	private static final Map<String, ZConfServiceInfo> EMPTY_SERVICEINFO_MAP = new ConcurrentHashMap<String, ZConfServiceInfo>();

	public Map<String, ZConfServiceInfo> getServiceInfoMap() {
		assert mStarted == true : "DTalkService not started.";
		// XXX Collections.unmodifiableMap?
		return mServiceDiscovery != null ? mServiceDiscovery.getServiceInfoMap() : EMPTY_SERVICEINFO_MAP;
	}

	/**
	 * Startup {@code DTalkService}.
	 */
	public void startup() {
		LOG.v(TAG, ">>> startup");

		mConfiguration.getThreadPool().execute(this);
	}

	@Override
	public void run() {
		LOG.v(TAG, ">>> run");

		synchronized (this) {
			if (mStarted) {
				LOG.w(TAG, "DTalkService already started.");
				return;
			}
			
			// Update state.
			mStarted = true;

			// We subscribe anyway, its simpler (no configuration check).
			LOG.d(TAG, "Subscribe for: %s", WebPresenceEvent.class.getName());
			MessageBus.subscribe(WebPresenceEvent.class.getName(), mWebPresenceEventListener);

			// Start DTalkServer...
			if (mDTalkServer != null) {
				try {
					mDTalkServer.startServer();
				} catch (Exception e) {
					LOG.e(TAG, "Failed to start DTalkServer: %s.", e.getMessage(), e);

					// Just shutdown...
					shutdown();

					// and return.
					return;
				}
			}

			// Publish DTalkService...
			publishService();

			// Start service discovery...
			if (mServiceDiscovery != null) {
				mServiceDiscovery.startup();
			}

			// Just print a message.
			LOG.i(TAG, "DTalkService started.");
		}
	}

	/**
	 * Shutdown {@code DTalkService}.
	 */
	public void shutdown() {
		LOG.v(TAG, ">>> shutdown");

		synchronized (this) {
			if (!mStarted) {
				LOG.w(TAG, "DTalkService not started.");
				return;
			}

			// Update state.
			mStarted = false;

			// We unsubscribe from web presence events (see startup code).
			LOG.d(TAG, "Unsubscribe from: %s", WebPresenceEvent.class.getName());
			MessageBus.unsubscribe(WebPresenceEvent.class.getName(), mWebPresenceEventListener);

			// Shutdown service discovery...
			if (mServiceDiscovery != null) {
				mServiceDiscovery.shutdown();
			}

			// Close client connections...
			DTalkConnectionRegistry.getInstance().reset();

			// Unpublish DTalkService...
			unpublishService();

			// Stop WebSocket server...
			if (mDTalkServer != null) {
				try {
					mDTalkServer.stopServer();
				} catch (Exception e) {
					LOG.e(TAG, e.getMessage());
				}
			}
			
			// Shutdown executor service.
			ExecutorService threadPool = DTalkService.getInstance().getConfiguration().getThreadPool();
			shutdownAndWaitTermination(threadPool, 3333L);

			// Just print a message.
			LOG.i(TAG, "DTalkService stopped.");
		}
	}

	/**
	 * Republish presence.
	 */
	public void republishService() {
		LOG.v(TAG, ">>> republishService");

		synchronized (this) {
			try {
				unpublishService();
			} finally {
				publishService();
			}
		}
	}

	// NOTE: must be called from inside synchronized(this) {..} block
	private void publishService() {
		LOG.v(TAG, ">>> publishService");

		String targetName = mConfiguration.getTargetName();
		if (targetName == null || targetName.trim().length() == 0) {
			return;
		}

		String deviceId = mConfiguration.getDeviceId();
		if (deviceId != null && deviceId.trim().length() > 0) {
			targetName += "@" + deviceId;
		}

		// register service on the allocated port
		Map<String, String> props = new HashMap<String, String>();
		props.put(DTalk.KEY_PRESENCE_DTALK, "1");
		props.put(DTalk.KEY_PRESENCE_DTYPE, mConfiguration.getType());
		// ...

		InetSocketAddress address = getSocketAddress();
		updateServiceInfo(new ZConfServiceInfo(targetName, DTalk.SERVICE_TYPE, props, address.getAddress(), address.getPort()));
	}

	// NOTE: must be called from inside synchronized(this) {..} block
	private void unpublishService() {
		LOG.v(TAG, ">>> unpublishService");

		updateServiceInfo(null);
	}

	// NOTE: must be called from inside synchronized(this) {..} block
	private synchronized void updateServiceInfo(ZConfServiceInfo serviceInfo) {
		LOG.v(TAG, ">>> updateServiceInfo: %s", serviceInfo);

		final ExecutorService threadPool = mConfiguration.getThreadPool();

		if (mZConfServiceInfo != null) {

			// Notify that DTalkService is about to unregister.
			MessageBus.sendMessage(new DTalkServiceEvent(), threadPool);

			// Unregister service.
			if (mConfiguration.registerService()) {
				mConfiguration.getZConfManager().unregisterService(mZConfRegistrationListener);
			}

			// Disable WebPresence...
			// threadPool.execute(new Runnable() {
			// @Override
			// public void run() {
			enableWebPresence(false);
			// }
			// });
		}

		mZConfServiceInfo = serviceInfo;

		if (mZConfServiceInfo != null) {

			// Notify that DTalkService is ready to handle local connections.
			MessageBus.sendMessage(new DTalkServiceEvent(mZConfServiceInfo), threadPool);

			// Publish service.
			if (mConfiguration.registerService()) {
				mConfiguration.getZConfManager().registerService(mZConfServiceInfo, mZConfRegistrationListener);
			}

			// Enable/disable WebPresence...
			// threadPool.execute(new Runnable() {
			// @Override
			// public void run() {
			enableWebPresence(mConfiguration.isWebPresenceEnabled());
			// }
			// });
		}
	}

	// --------------------------------------------------------------------------
	// WebPresence
	// --------------------------------------------------------------------------

	private volatile WebPresenceService mWebPresenceService = null;

	private WebPresenceService getWebPresenceService() {
		if (mWebPresenceService == null) {
			synchronized (WebPresenceService.class) {
				if (mWebPresenceService == null) {
					mWebPresenceService = new WebPresenceService();
				}
			}
		}
		return mWebPresenceService;
	}

	/**
	 * Enabled/disable WebPresence.
	 * <p>
	 * NOTE: to be run in a separate thread.
	 * </p>
	 * 
	 * @param webPresence
	 */
	public void enableWebPresence(boolean webPresence) {
		LOG.v(TAG, ">>> enableWebPresence: %b", webPresence);

		if (webPresence) {
			try {
				final String webPresenceURI = mConfiguration.getWebPresenceURL();
				if (webPresenceURI == null || webPresenceURI.trim().length() == 0) {
					return;
				}
				// getWebPresenceService().publish(new URI(webPresenceURI),
				// mConfiguration.getJmDNS(), getLocalServiceInfo());
				// TODO getWebPresenceService().publish(new URI(webPresenceURI),
				// mConfiguration.getNsdManager(), getLocalServiceInfo());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				getWebPresenceService().unpublish();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * NOTE: This event will not get fired after a call to {@link #shutdown()}.
	 */
	protected void onWebPresenceClosed() {
		mConfiguration.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3333L);
				} catch (InterruptedException e) {
					// ignore
				}

				// WebPresence reconnection...
				enableWebPresence(mConfiguration.isWebPresenceEnabled());
			}
		});
	}

	// --------------------------------------------------------------------------
	// Utility methods
	// --------------------------------------------------------------------------

	public InetSocketAddress getSocketAddress() {
		if (mDTalkServer != null) {
			return mDTalkServer.getSocketAddress();
		} else {
			return mConfiguration.getSocketAddress();
		}
	}

	public String getLocalServiceAddress() {
		// NOTE: uses locking
		return getWebSocketAddress(getLocalServiceInfo());
	}

	public String getServiceAddressForLocalhost() {
		// NOTE: avoids locking.
		final InetSocketAddress address = getSocketAddress();
		StringBuilder sb = new StringBuilder();
		sb.append("ws://localhost:");
		sb.append(address.getPort());
		sb.append(DTalkServer.DTALKSRV_PATH);
		return sb.toString();
	}

	public static String getWebSocketAddress(ZConfServiceInfo info) {
		StringBuilder sb = new StringBuilder();
		sb.append("ws://");
		sb.append(info.getHost().getHostAddress()).append(':').append(info.getPort());
		sb.append(DTalkServer.DTALKSRV_PATH);
		return sb.toString();
	}

	@Deprecated
	public String getLocalResourceAddress(File resource) {
		ZConfServiceInfo info = getLocalServiceInfo();

		StringBuilder sb = new StringBuilder();
		sb.append("http://");
		sb.append(info.getHost().getHostAddress()).append(':').append(info.getPort());
		sb.append(resource.getAbsolutePath());
		return sb.toString();
	}
	
	/**
	 * Shuts down an ExecutorService in two phases, first by calling shutdown to
	 * reject incoming tasks, and then calling shutdownNow, if necessary, to
	 * cancel any lingering tasks.
	 */
	private static void shutdownAndWaitTermination(ExecutorService pool, long timeout) {
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
					System.err.println("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

}

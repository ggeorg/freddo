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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.arkasoft.freddo.dtalk.DTalkConnectionRegistry;
import com.arkasoft.freddo.dtalk.DTalkDispatcher;
import com.arkasoft.freddo.dtalk.netty4.server.WebPresenceService;
import com.arkasoft.freddo.dtalk.netty4.server.WebSocketServer;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.events.DTalkServiceEvent;
import freddo.dtalk.events.WebPresenceEvent;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfManager;
import freddo.dtalk.zeroconf.ZConfRegistrationListener;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

public class DTalkService {
	private static final String TAG = LOG.tag(DTalkService.class);

	public static final String LOCAL_CHANNEL_PREFIX = "dtalk-";

	/**
	 * {@code DTalkService} configuration object.
	 */
	public static interface Configuration {

		ZConfManager getNsdManager();

		String getDeviceId();

		String getType();

		String getTargetName();

		ExecutorService getThreadPool();

		boolean isWebPresenceEnabled();

		String getWebPresenceURL();

		int getPort();

		byte[] getHardwareAddress();

		String getHardwareAddress(String separator);

		InetAddress getInetAddress();

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
					sInstance = new DTalkService(config);
					return sInstance;
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
	private final ZConfRegistrationListener mNsdRegistrationListener = new ZConfRegistrationListener() {
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
			DTalkService.this.mNsdServiceInfo = serviceInfo;
		}

		@Override
		public void onServiceUnregistered(ZConfServiceInfo serviceInfo) {
			LOG.e(TAG, ">>> onRegistrationFailed: %s", serviceInfo);
			DTalkService.this.mNsdServiceInfo = null;
		}
	};

	// --------------------------------------------------------------------------

	private final Configuration mConfiguration;
	private final WebSocketServer mWebSocketServer;

	private boolean mStarted = false;

	private DTalkDiscovery mServiceDiscovery = null;
	private ZConfServiceInfo mNsdServiceInfo = null;

	/**
	 * {@code DTalkService} constructor.
	 * 
	 * @param configuration
	 */
	private DTalkService(Configuration configuration) {
		mConfiguration = configuration;
		mWebSocketServer = new WebSocketServer();

		// Start dispatcher...
		DTalkDispatcher.start();
	}

	public Configuration getConfiguration() {
		return mConfiguration;
	}

	public ZConfServiceInfo getLocalServiceInfo() {
		synchronized (this) {
			return mNsdServiceInfo;
		}
	}

	// In case there is no discovery running this map can be used as the roster (experimental).
	private static final Map<String, ZConfServiceInfo> EMPTY_SERVICEINFO_MAP = new ConcurrentHashMap<String, ZConfServiceInfo>();

	public Map<String, ZConfServiceInfo> getServiceInfoMap() {
		synchronized (this) {
			return mServiceDiscovery != null ? Collections.unmodifiableMap(mServiceDiscovery.mServiceInfoMap) : EMPTY_SERVICEINFO_MAP;
		}
	}

	/**
	 * Startup {@code DTalkService}.
	 */
	public void startup() {
		LOG.v(TAG, ">>> startup");

		synchronized (this) {
			if (mStarted) {
				LOG.w(TAG, "DTalkService already started.");
				return;
			}
			mStarted = true; // TODO move it to the end

			if (getConfiguration().runServiceDiscovery()) {
				if (mServiceDiscovery != null) {
					mServiceDiscovery.shutdown();
					mServiceDiscovery = null;
				}
				mServiceDiscovery = new DTalkDiscovery();
			}

			LOG.d(TAG, "Subscribe for: %s", WebPresenceEvent.class.getName());
			MessageBus.subscribe(WebPresenceEvent.class.getName(), mWebPresenceEventListener);

			mConfiguration.getThreadPool().execute(new Runnable() {
				@Override
				public void run() {
					try {
						// this will block execution until server is down...
						mWebSocketServer.startup(new Runnable() {
							@Override
							public void run() {
								// Publish DTalkService...
								synchronized (DTalkService.this) {
									publishService();
								}

								if (getConfiguration().runServiceDiscovery()) {
									// Start the service discovery...
									if (mServiceDiscovery != null) {
										mServiceDiscovery.startup();
									}
								}

								LOG.i(TAG, "DTalkService is started.");
							}
						});

						// server is down...
						shutdown();
					} catch (Throwable t) {
						LOG.e(TAG, "Uncaught exception in WebSocketServer startup: ", t);
					}
				}
			});
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
			mStarted = false; // move this to the end

			try {
				LOG.d(TAG, "Unsubscribe from: %s", WebPresenceEvent.class.getName());
				MessageBus.unsubscribe(WebPresenceEvent.class.getName(), mWebPresenceEventListener);
			} catch (Exception e) {
				// ignore
			}

			// Shutdown service discovery...
			if (mServiceDiscovery != null) {
				mServiceDiscovery.shutdown();
				mServiceDiscovery = null;
			}

			// Close client connections...
			DTalkConnectionRegistry.getInstance().reset();

			// Stop WebSocket server...
			unpublishService();
			mWebSocketServer.shutdown();
		}

		LOG.i(TAG, "DTalkService is down.");
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

		try {
			InetSocketAddress address = mWebSocketServer.getAddress();
			updateServiceInfo(new ZConfServiceInfo(targetName, DTalk.SERVICE_TYPE, props, mConfiguration.getInetAddress(), address.getPort()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// NOTE: must be called from inside synchronized(this) {..} block
	private void unpublishService() {
		LOG.v(TAG, ">>> unpublishService");

		try {
			updateServiceInfo(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// NOTE: must be called from inside synchronized(this) {..} block
	private synchronized void updateServiceInfo(ZConfServiceInfo serviceInfo) throws IOException {
		LOG.v(TAG, ">>> updateServiceInfo: %s", serviceInfo);

		ExecutorService threadPool = getConfiguration().getThreadPool();

		if (mNsdServiceInfo != null) {

			// Notify that DTalkService is about to unregister.
			MessageBus.sendMessage(new DTalkServiceEvent(), threadPool);

			// Unregister service.
			if (getConfiguration().registerService()) {
				mConfiguration.getNsdManager().unregisterService(mNsdRegistrationListener);
				// mConfiguration.getJmDNS().unregisterService(mLocalServiceInfo);
			}

			// Disable WebPresence...
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					enableWebPresence(false);
				}
			});
		}

		mNsdServiceInfo = serviceInfo;

		if (mNsdServiceInfo != null) {

			// Notify that DTalkService is ready to handle local connections.
			MessageBus.sendMessage(new DTalkServiceEvent(mNsdServiceInfo), threadPool);

			// Publish service.
			if (getConfiguration().registerService()) {
				mConfiguration.getNsdManager().registerService(mNsdServiceInfo, mNsdRegistrationListener);
				// mConfiguration.getJmDNS().registerService(mLocalServiceInfo);
			}

			// Enable/disable WebPresence...
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					enableWebPresence(mConfiguration.isWebPresenceEnabled());
				}
			});
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

	public InetSocketAddress getWebSocketServerAddress() {
		return mWebSocketServer.getAddress();
	}

	public String getLocalServiceAddress() {
		// NOTE: uses locking
		return getWebSocketAddress(getLocalServiceInfo());
	}

	public String getServiceAddressForLocalhost() {
		// NOTE: avoids locking.
		final InetSocketAddress address = getWebSocketServerAddress();
		StringBuilder sb = new StringBuilder();
		sb.append("ws://localhost:");
		sb.append(address.getPort());
		sb.append(WebSocketServer.WEBSOCKET_PATH);
		return sb.toString();
	}

	public static String getWebSocketAddress(ZConfServiceInfo info) {
		StringBuilder sb = new StringBuilder();
		sb.append("ws://");
		sb.append(info.getHost().getHostAddress()).append(':').append(info.getPort());
		sb.append(WebSocketServer.WEBSOCKET_PATH);
		return sb.toString();
	}

}

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
package com.arkasoft.freddo.dtalk.j7ee.server;

import java.util.concurrent.ExecutorService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceConfiguration;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfManager;

public abstract class DTalkContextListener implements ServletContextListener, DTalkServiceContext {
	private static final String TAG = LOG.tag(DTalkContextListener.class);

	public static final String CONFIG_DTALK_PORT = "dtalksrv.port";
	public static final String CONFIG_REMOTE_ADDR_POLICY = "dtalk.remoteAddr.policy";

	/** DTalkConnectionEvent listener. */
	private final MessageBusListener<DTalkConnectionEvent> dtalkConnectionEL = new MessageBusListener<DTalkConnectionEvent>() {
		@Override
		public void messageSent(String topic, DTalkConnectionEvent message) {
			try {
				DTalkServerEndpoint conn = message.getConnection();
				if (message.isOpen()) {
					onConnectionOpen(conn);
				} else {
					onConnectionClose(conn);
				}
			} catch (Throwable t) {
				LOG.e(TAG, "Uncaughted exception: %s", t.getMessage(), t);
			}
		}
	};

	protected void onConnectionOpen(DTalkServerEndpoint conn) {
		// do nothing here
	}

	protected void onConnectionClose(DTalkServerEndpoint conn) {
		// do nothing here
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.v(TAG, ">>> contextInitialized");

		DTalkService.init(getConfiguration(sce));
		DTalkService.getInstance().startup();

		MessageBus.subscribe(DTalkConnectionEvent.class.getName(), dtalkConnectionEL);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.v(TAG, ">>> contextDestroyed");

		// Shutdown DTalkService
		DTalkService.getInstance().shutdown();
	}

	@Override
	public void runOnUiThread(Runnable r) {
		r.run();
	}

	@Override
	public void assertBackgroundThread() {
		// TODO Auto-generated method stub

	}

	@Override
	public ExecutorService getThreadPool() {
		return DTalkService.getInstance().getConfiguration().getThreadPool();
	}

	protected DTalkService.Configuration getConfiguration(ServletContextEvent sce) {
		final ServletContext ctx = sce.getServletContext();
		return new DTalkServiceConfiguration() {
			@Override
			public boolean isHosted() {
				return true;
			}

			@Override
			public String getType() {
				return "DTalkServer/1";
			}

			@Override
			public ZConfManager getZConfManager() {
				return null;
			}

			@Override
			public boolean runServiceDiscovery() {
				return false;
			}

			@Override
			public boolean registerService() {
				return false;
			}

			@Override
			public int getPort() {
				return Integer.parseInt((String) ctx.getInitParameter(CONFIG_DTALK_PORT));
			}
		};
	}
}

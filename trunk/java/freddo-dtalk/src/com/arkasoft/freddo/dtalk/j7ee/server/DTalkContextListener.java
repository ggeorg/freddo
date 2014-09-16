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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceConfiguration;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfManager;

public abstract class DTalkContextListener implements ServletContextListener, DTalkServiceContext {
	private static final String TAG = LOG.tag(DTalkContextListener.class);

	// private final Map<String, DTalkConnectionImpl> mConnections = new
	// ConcurrentHashMap<String, DTalkConnectionImpl>();

	/** DTalkConnectionEvent listener. */
	private final MessageBusListener<DTalkConnectionEvent> dtalkConnectionEL = new MessageBusListener<DTalkConnectionEvent>() {
		@Override
		public void messageSent(String topic, DTalkConnectionEvent message) {
			DTalkServerEndpoint conn = message.getConnection();
			if (message.isOpen()) {
				onConnectionOpen(conn);
			} else {
				onConnectionClose(conn);
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

		//
		// TODO read configuration settings
		//

		DTalkService.init(getDTalkServiceConfiguration(sce));
		DTalkService.getInstance().startup();

		// MessageBus.subscribe(DTalkConnectionEvent.class.getName(),
		// dtalkConnectionEL);
		// MessageBus.subscribe(OutgoingMessageEvent.class.getName(),
		// outgoingEventListener);
		// MessageBus.subscribe(IncomingMessageEvent.class.getName(),
		// incomingEventListener);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.v(TAG, ">>> contextDestroyed");

		// Shutdown DTalkService
		DTalkService.getInstance().shutdown();
	}

	protected DTalkService.Configuration getDTalkServiceConfiguration(ServletContextEvent sce) {
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
				return Integer.parseInt((String) ctx.getInitParameter("dtalksrv.port"));
			}
		};
	}
}

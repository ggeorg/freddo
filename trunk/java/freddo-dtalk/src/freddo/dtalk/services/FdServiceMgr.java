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
package freddo.dtalk.services;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.util.LOG;

public class FdServiceMgr extends FdService {

	/* A map to register all running services. */
	private final Map<String, FdService> mServices;

	/**
	 * Service manager constructor.
	 * 
	 * @param context
	 *          the service runtime context.
	 */
	public FdServiceMgr(DTalkServiceContext context) {
		super(context, "dtalk.Services");
		mServices = new ConcurrentHashMap<String, FdService>();
	}

	/**
	 * Register a service with service manager.
	 * 
	 * @param service
	 */
	public void registerService(FdService service) {
		if (mServices.containsValue(service.getName())) {
			throw new IllegalStateException("Service '" + service.getName() + "' was registered before.");
		}
		mServices.put(service.getName(), service);
	}

	@Override
	protected void onDisposed() {
		LOG.v(getName(), ">>> onDisposed");

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				LOG.d(getName(), "Calling dispose() on each service...");

				Iterator<Map.Entry<String, FdService>> serviceIter = mServices.entrySet().iterator();
				while (serviceIter.hasNext()) {
					FdService service = serviceIter.next().getValue();
					serviceIter.remove();
					try {
						service.dispose();
					} catch (Throwable t) {
						LOG.e(getName(), t.getMessage(), t);
					}
				}
			}
		});
	}

	// -----------------------------------------------------------------------
	// SERVICE MANAGER API
	// -----------------------------------------------------------------------

	/**
	 * GET SERVICES request handler.
	 * 
	 * @param request
	 *          the request.
	 */
	public final void getServices(JSONObject request) {
		LOG.v(getName(), ">>> getServices");

		try {
			sendResponse(request, getServices());
		} catch (DTalkException e) {
			sendErrorResponse(request, e.getCode(), e.getMessage(), e.getData());
		}
	}

	/**
	 * GET SERVICES implementation.
	 */
	protected JSONArray getServices() throws DTalkException {
		JSONArray result = new JSONArray();
		for (String srvName : mServices.keySet()) {
			try {
				JSONObject service = new JSONObject();
				service.put("name", srvName);
				result.put(service);
			} catch (JSONException e) {
				throw new DTalkException(DTalkException.INTERNAL_ERROR, e.getMessage());
			}
		}
		return result;
	}

	// -----------------------------------------------------------------------

	@Deprecated
	public void doStart(JSONObject request) {
		LOG.v(getName(), ">>> doStart: %s", request);
		String service = request.optString(MessageEvent.KEY_BODY_PARAMS, "");
		sendResponse(request, mServices.containsKey(service));
	}

	@Deprecated
	public void doStop(JSONObject notification) {
		LOG.v(getName(), ">>> doStop: %s", notification);
		// do nothing
	}

}

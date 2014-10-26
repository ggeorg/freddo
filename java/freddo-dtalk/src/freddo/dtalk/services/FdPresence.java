package freddo.dtalk.services;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

/**
 * Presence service.
 * 
 * <p>
 * Getters:
 * <ul>
 * <li>{@code roster}</li>
 * </ul>
 * <p>
 * Methods:
 * <ul>
 * <li>addToRoster</li>
 * </ul>
 * <p>
 * Events:
 * <ul>
 * <li>{@code onresolved}</li>
 * <li>{@code onremoved}</li>
 * </ul>
 */
public class FdPresence extends FdService {

	private MessageBusListener<JSONObject> mDtalkPresenceListener;

	public FdPresence(DTalkServiceContext activity) {
		super(activity, DTalk.DEFAULT_SRV_PREFIX + "Presence");

		mDtalkPresenceListener = new MessageBusListener<JSONObject>() {
			@Override
			public void messageSent(String topic, JSONObject message) {
				try {
					if (message.get(DTalk.KEY_BODY_ACTION).equals(DTalk.ACTION_RESOLVED)) {
						JSONObject params = message.getJSONObject(DTalk.KEY_BODY_PARAMS);
						LOG.d(getName(), "Presence resolved: %s", params);
						FdPresence.this.fireEvent("onresolved", params);
					} else if (message.get(DTalk.KEY_BODY_ACTION).equals(DTalk.ACTION_REMOVED)) {
						JSONObject params = message.getJSONObject(DTalk.KEY_BODY_PARAMS);
						LOG.d(getName(), "Presence removed: %s", params);
						FdPresence.this.fireEvent("onremoved", params);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};

		MessageBus.subscribe(DTalk.SERVICE_DTALK_PRESENCE, mDtalkPresenceListener);
	}

	@Override
	protected void onDisposed() {
		LOG.v(getName(), ">>> reset");

		if (mDtalkPresenceListener != null) {
			LOG.v(getName(), "Unregister listener: %s", DTalk.SERVICE_DTALK_PRESENCE);
			MessageBus.unsubscribe(DTalk.SERVICE_DTALK_PRESENCE, mDtalkPresenceListener);
			mDtalkPresenceListener = null;
		}
	}

	// -----------------------------------------------------------------------
	// GET ROSTER
	// -----------------------------------------------------------------------

	/**
	 * Get roster request handler.
	 * 
	 * @param request
	 *          the request.
	 */
	public void getRoster(JSONObject request) {
		LOG.v(getName(), ">>> getList");
		sendResponse(request, getRoster());
	}

	/**
	 * Get roster request handler implementation.
	 * 
	 * @return a {@code JSONArray} that contains the current roster.
	 */
	protected JSONArray getRoster() {
		JSONArray result = new JSONArray();

		Map<String, ZConfServiceInfo> serviceInfoMap = DTalkService.getInstance().getServiceInfoMap();
		for (String key : serviceInfoMap.keySet()) {
			try {
				result.put(ZConfServiceInfo.serviceInfoToJSON(serviceInfoMap.get(key)));
			} catch (JSONException e) {
				LOG.w(getName(), e.getMessage());
			}
		}

		return result;
	}

	// -----------------------------------------------------------------------
	// ADD TO ROSTER
	// -----------------------------------------------------------------------

	/**
	 * ADD TO ROSTER request handler.
	 * 
	 * @param request
	 *          the request.
	 */
	public void doAddToRoster(JSONObject request) {
		LOG.v(getName(), ">>> getList");

		try {
			sendResponse(request, addToRoster(ZConfServiceInfo.jsonToServiceInfo(request.getJSONObject(DTalk.KEY_BODY_PARAMS))));
		} catch (JSONException e) {
			sendErrorResponse(request, DTalkException.INVALID_JSON, e.getMessage());
		} catch (Exception e) {
			sendErrorResponse(request, DTalkException.INTERNAL_ERROR, e.getMessage());
		}
	}

	protected boolean addToRoster(ZConfServiceInfo serviceInfo) {
		DTalkService.getInstance().getServiceInfoMap().put(serviceInfo.getServiceName(), serviceInfo);
		return true;
	}
	
	// -----------------------------------------------------------------------

	@Deprecated
	public void getList(JSONObject request) {
		LOG.v(getName(), ">>> getList");
		sendResponse(request, getRoster());
	}

}

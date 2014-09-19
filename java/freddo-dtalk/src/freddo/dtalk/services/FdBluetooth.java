package freddo.dtalk.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.AsyncCallback;
import freddo.dtalk.util.LOG;

/**
 * 
 */
public abstract class FdBluetooth extends FdService {
	
	public static final Integer NOT_DISCOVERABLE = Integer.valueOf(0);
	public static final Integer LIAC = Integer.valueOf(10390272);
	public static final Integer GIAC = Integer.valueOf(10390323);

	protected FdBluetooth(DTalkServiceContext context) {
		super(context, DTalk.DEFAULT_SRV_PREFIX + "Bluetooth");
	}

	/**
	 * 
	 * @param request
	 */
	public void getDevices(final JSONObject request) {
		LOG.v(mName, ">>> getDevices");

		try {

			getDevices(new AsyncCallback<JSONArray>() {
				@Override
				public void onFailure(Throwable caught) {
					sendErrorResponse(request, DTalkException.INTERNAL_ERROR, caught.getMessage());
				}

				@Override
				public void onSuccess(JSONArray response) {
					sendResponse(request, response);
				}
			});

		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract void getDevices(AsyncCallback<JSONArray> callback) throws DTalkException;

	/**
	 * Querying paired devices.
	 * <p>
	 * This will return a set of Bluetooth devices representing paired devices.
	 * 
	 * @param request
	 *          The incoming request.
	 */
	public void getBondedDevices(JSONObject request) {
		LOG.v(mName, ">>> getBondedDevices");

		try {
			sendResponse(request, getBondedDevices());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract JSONArray getBondedDevices() throws DTalkException;

	/**
	 * 
	 * @param request
	 */
	public void getEnabled(JSONObject request) {
		LOG.v(mName, ">>> getEnabled");

		try {
			sendResponse(request, getEnabled());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract Boolean getEnabled() throws DTalkException;

	public void getDiscoverable(JSONObject request) {
		LOG.v(mName, ">>> getDiscoverable");

		try {
			sendResponse(request, getDiscoverable());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract Integer getDiscoverable() throws DTalkException;

	/**
	 * 
	 * @param request
	 */
	public void getBondState(JSONObject request) {
		LOG.v(mName, ">>> getBoundState");

		try {
			String address = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, getBondState(address));
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		} catch (JSONException e) {
			sendErrorResponse(request, DTalkException.INVALID_PARAMS, e.getMessage());
		}
	}

	protected abstract Boolean getBondState(String address) throws DTalkException;

	/**
	 * 
	 * @param request
	 */
	public void doCreateBond(JSONObject request) {
		LOG.v(mName, ">>> doPair");

		try {
			String address = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, createBond(address));
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		} catch (JSONException e) {
			sendErrorResponse(request, DTalkException.INVALID_PARAMS, e.getMessage());
		}
	}

	protected abstract Boolean createBond(String address) throws DTalkException;

	/**
	 * 
	 * @param request
	 */
	public void doRemoveBond(JSONObject request) {
		LOG.v(mName, ">>> doUnPair");

		try {
			String address = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, removeBond(address));
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		} catch (JSONException e) {
			sendErrorResponse(request, DTalkException.INVALID_PARAMS, e.getMessage());
		}
	}

	protected abstract Boolean removeBond(String address) throws DTalkException;
}

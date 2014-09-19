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
public abstract class FdBluetooh extends FdService {

	protected FdBluetooh(DTalkServiceContext context, String name) {
		super(context, name);
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
   * @param request The incoming request.
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
  public void doEnable(JSONObject request) {
    LOG.v(mName, ">>> doEnable");

    try {
      sendResponse(request, enable());
    } catch (DTalkException e) {
      sendErrorResponse(request, e);
    }
  }

  protected abstract Boolean enable() throws DTalkException;
  
  /**
   * 
   * @param request
   */
  public void doDisable(JSONObject request) {
    LOG.v(mName, ">>> doDsiable");

    try {
      sendResponse(request, disable());
    } catch (DTalkException e) {
      sendErrorResponse(request, e);
    }
  }

  protected abstract Boolean disable() throws DTalkException;
  
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

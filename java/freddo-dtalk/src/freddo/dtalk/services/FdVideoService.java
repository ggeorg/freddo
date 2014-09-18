package freddo.dtalk.services;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.LOG;

public abstract class FdVideoService extends FdUiService {

	protected FdVideoService(DTalkServiceContext context) {
		super(context, DTalk.DEFAULT_SRV_PREFIX + "Video");
	}

	@Override
	protected void onDisposed() {
		// TODO Auto-generated method stub

	}

	public void getInfo(JSONObject request) {
		try {
			sendResponse(request, getInfo());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract JSONObject getInfo() throws DTalkException;

	public void getVolume(JSONObject request) {
		try {
			sendResponse(request, getVolume());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract Object getVolume() throws DTalkException;

	public void getItem(JSONObject request) {
		try {
			sendResponse(request, getItem());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract JSONObject getItem() throws DTalkException;

	public void setSrc(JSONObject options) {
		try {
			setSrc(options.getString("src"));
		} catch (JSONException e) {
			LOG.e(mName, e.getMessage(), e);
		}
	}

	protected abstract void setSrc(String string);

	public void setStartPositionPercent(JSONObject options) {
		try {
			setStartTimePercent(options.getDouble("startPositionPercent"));
		} catch (JSONException e) {
			LOG.e(mName, e.getMessage(), e);
		}
	}

	protected abstract void setStartTimePercent(double startPositionPercent);

	public void setItem(JSONObject options) {
		try {
			setItemImpl(options.getJSONObject("item"));
		} catch (JSONException e) {
			LOG.e(mName, e.getMessage(), e);
		}
	}

	protected abstract void setItemImpl(JSONObject jsonObject);
	
	public void doPlay(JSONObject request) {
    LOG.v(mName, ">>> doPlay");
    
    try {
			sendResponse(request, play());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}
	
	protected abstract boolean play() throws DTalkException;

	public void doPause(JSONObject request) {
		LOG.v(mName, ">>> doPause");

		try {
			sendResponse(request, pause());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract boolean pause() throws DTalkException;

}

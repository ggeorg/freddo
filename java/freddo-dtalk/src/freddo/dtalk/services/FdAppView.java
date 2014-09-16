package freddo.dtalk.services;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.util.LOG;

public abstract class FdAppView extends FdUiService {

	protected FdAppView(DTalkServiceContext context, JSONObject options) {
		super(context, DTalk.DEFAULT_SRV_PREFIX + "AppView", options);
	}

	@Override
	protected void onDisposed() {
		LOG.v(mName, ">>> onDisposed");
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setUrl("about:blank");
			}
		});
	}

	public void setUrl(JSONObject options) {
		LOG.v(mName, ">>> setUrl");

		try {
			setUrl(options.getString("url"));
		} catch (JSONException e) {
			LOG.e(mName, e.getMessage(), e);
		}
	}

	protected abstract void setUrl(String url);

}

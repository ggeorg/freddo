package freddo.dtalk.services;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;

public abstract class FdUiService extends FdService {

	protected FdUiService(DTalkServiceContext context, String name, JSONObject options) {
		super(context, name, options);
	}

}

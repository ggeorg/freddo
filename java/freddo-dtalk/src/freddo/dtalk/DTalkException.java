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

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.util.LOG;

/**
 * Generic DTalk exception.
 * <p>
 * Error codes are similar to JSON-RPC 2: http://www.jsonrpc.org/specification
 * </p>
 */
public class DTalkException extends Exception {
	private static final long serialVersionUID = 5691944770816935981L;
	private static final String TAG = LOG.tag(DTalkException.class);

	/**
	 * Parse error Invalid JSON was received by the server. An error occurred on
	 * the server while parsing the JSON text.
	 */
	public static final int INVALID_JSON = -32700;

	/**
	 * Invalid Request The JSON sent is not a valid Request object.
	 */
	public static final int INVALID_REQUEST = -32600;

	/**
	 * Method not found The method does not exist / is not available.
	 */
	public static final int METHOD_NOT_FOUND = -32601;

	/**
	 * Invalid params Invalid method parameter(s).
	 */
	public static final int INVALID_PARAMS = -32602;

	/**
	 * Internal error Internal JSON-RPC error.
	 */
	public static final int INTERNAL_ERROR = -32603;

	/**
	 * Request timeout.
	 */
	public static final int REQUEST_TIMEOUT = -32699;

	private final int mCode;
	private final JSONObject mData;

	public DTalkException(int code, String message) {
		this(code, message, null);
	}

	public DTalkException(int code, String message, JSONObject data) {
		super(message);
		mCode = code;
		mData = data;
	}

	public int getCode() {
		return mCode;
	}

	public JSONObject getData() {
		return mData;
	}

	// --------------------------------------------------------------------------
	// JSON serialization
	// --------------------------------------------------------------------------

	private JSONObject mJSON = null;

	public JSONObject toJSON() {
		if (mJSON == null) {
			mJSON = new JSONObject();
			try {
				mJSON.put(DTalk.KEY_ERROR_CODE, getCode());
				mJSON.put(DTalk.KEY_ERROR_MESSAGE, getMessage());
				if (mData != null) {
					mJSON.put(DTalk.KEY_ERROR_DATA, mData);
				}
			} catch (JSONException e) {
				LOG.e(TAG, e.getMessage());
			}
		}
		return mJSON;
	}

}

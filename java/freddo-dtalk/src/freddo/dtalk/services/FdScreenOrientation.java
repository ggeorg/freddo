package freddo.dtalk.services;

import org.json.JSONObject;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

/**
 * The screen orientation API provides the ability to read the screen
 * orientation state, to be informed when this state changes, and to be able to
 * lock the screen orientation to a specific state.
 * <p>
 * The current orientation is represensed by one of the following values:
 * <ul>
 * <li>{@code portrait-primary} The Orientation is in the primary portrait mode.
 * </li>
 * </ul>
 * 
 * <p>
 * The concepts of primary orientation and secondary orientation depends on the
 * device and the platform. Some devices are in portrait mode when the user
 * holds the device in the normal orientation, other devices are in landscape
 * mode when the user holds the device in its normal orientation. For devices
 * whose normal orientation is a landscape mode, that normal orientation should
 * be represented as landscape-primary. For devices whose normal orientation is
 * a portrait mode, that normal orientation should be represented as
 * portrait-primary. In both if the device is in landscape-primary and is
 * rotated 90 degrees clockwise, that should be represented as portrait-primary.
 * <p>
 * To lock the orientation means forcing the rendering of the current browsing
 * context to behave as if the screen was naturally orientated in a given
 * orientation and will only change orientation to a given set of allowed
 * orientations, regardless of the actual screen orientation relative to the
 * user or the previous locks.
 * <p>
 * ...
 */
public abstract class FdScreenOrientation extends FdService {

	public static final String SRV_NAME = DTalk.DEFAULT_SRV_PREFIX + "ScreenOrientation";

	/**
	 * Screen Orientation Constants
	 */

	public static final String UNLOCKED = "unlocked";

	/** The orientation is in the primary portrait mode. */
	public static final String PORTRAIT_PRIMARY = "portrait-primary";
	/** The orientation is in the secondary portrait mode. */
	public static final String PORTRAIT_SECONDARY = "portrait-secondary";
	/** The orientation is in the primary landscape mode. */
	public static final String LANDSCAPE_PRIMARY = "landscape-primary";
	/** The orientation is in the secondary landscape mode. */
	public static final String LANDSCAPE_SECONDARY = "landscape-secondary";

	/** The orientation is either portrait-primary or portrait-secondary. */
	public static final String PORTRAIT = "portrait";
	/** The orientation is either landscape-primary or landscape-secondary. */
	public static final String LANDSCAPE = "landscape";

	protected FdScreenOrientation(DTalkServiceContext context, JSONObject options) {
		super(context, SRV_NAME, options);
	}

	// -----------------------------------------------------------------------
	// GET ORIENTATION
	// -----------------------------------------------------------------------

	public void getOrientation(JSONObject request) {
		LOG.v(SRV_NAME, ">>> getOrientation");

		try {
			sendResponse(request, getOrientation());
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		}
	}

	protected abstract String getOrientation() throws DTalkException;

	// -----------------------------------------------------------------------
	// LOCK ORIENTATION
	// -----------------------------------------------------------------------

	public void setOrientation(JSONObject options) {
		LOG.v(SRV_NAME, ">>> setOrientation");

		try {
			String orientation = options.getString("orientation");
			LOG.d(SRV_NAME, "Requested ScreenOrientation: %s", orientation);
			setOrientation(orientation);
		} catch (Exception e) {
			LOG.e(SRV_NAME, e.getMessage());
		}
	}

	protected abstract void setOrientation(String orientation) throws DTalkException;

}

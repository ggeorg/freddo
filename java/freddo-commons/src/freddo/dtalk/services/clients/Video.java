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
package freddo.dtalk.services.clients;

import org.json.JSONException;
import org.json.JSONObject;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkEventListener;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkSubscribeHandle;
import freddo.dtalk.util.AsyncCallback;
import freddo.dtalk.util.LOG;

public class Video extends Service {
	private static final String TAG = LOG.tag(Video.class);

  public Video() {
    this(null);
  }

  public Video(String target) {
    super("dtalk.service.Video", target);
  }

  public void getInfo(AsyncCallback<JSONObject> callback) {
    try {
      DTalkServiceAdapter.get(this, "info", callback);
    } catch (DTalkException e) {
      callback.onFailure(e);
    }
  }

  public void getSrc(AsyncCallback<String> callback) {
    try {
      DTalkServiceAdapter.get(this, "src", callback);
    } catch (DTalkException e) {
      callback.onFailure(e);
    }
  }

  public void setSrc(String src) throws DTalkException {
  	LOG.d(TAG, ">>> setSrc: %s", src);
  	
    DTalkServiceAdapter.set(this, "src", src);
  }

  public void setSrc(String src, double startPositionPercent) throws DTalkException {
  	LOG.d(TAG, ">>> setSrc: %s (%f)", src, startPositionPercent);
  	
    try {
      JSONObject options = new JSONObject();
      options.put("src", src);
      options.put("startPositionPercent", startPositionPercent);
      DTalkServiceAdapter.set(this, options);
    } catch (JSONException e) {
      throw new DTalkException(DTalkException.INVALID_JSON, e.getMessage());
    }
  }

  /**
   * Pauses playback of the current item.
   * <p>
   * If playback is not currently underway, this method has no effect. To resume
   * playback of the current item from the pause point, call the play method.
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void pause() throws DTalkException  {
    DTalkServiceAdapter.invoke(this, "pause");
  }

  /**
   * Moves the playhead to the new location.
   * <p>
   * For video-on-demand or progressively downloaded content, this value is
   * measured in seconds from the beginning of the current item. For content
   * streamed live from a server, this value represents the time from the
   * beginning of the playlist when it was first loaded.
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void seekTo(double sec) throws DTalkException  {
    DTalkServiceAdapter.invoke(this, "seekTo", sec);
  }

  /**
   * Initiates playback of the current item.
   * <p>
   * If playback was previously paused, this method resumes playback where it
   * left; otherwise, this method plays the first available item, from the
   * beginning.
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void play() throws DTalkException  {
    DTalkServiceAdapter.invoke(this, "play");
  }

  /**
   * Ends playback of the current item.
   * 
   * This method stops playback of the current item and resets the playhead to
   * the start of the item. Calling the play method again initiates playback
   * from the beginning of the item.
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void stop() throws DTalkException  {
    DTalkServiceAdapter.invoke(this, "stop");
  }

  /**
   * Change the playback rate. The value argument is a float value representing
   * the playback rate: 0 is paused, 1 is playing at the normal speed. If the
   * video implementation allows fast forward then it will work with values like
   * 2,3 etc.
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void setRate(double rate) throws DTalkException  {
    DTalkServiceAdapter.invoke(this, "setRate", rate);
  }

  /**
   * Send an image to render in the view
   * @throws DTalkException 
   * 
   * @throws JSONException
   */
  public void setPhoto(String photoUrl) throws DTalkException  {
    DTalkServiceAdapter.set(this, "photo", photoUrl);
  }

  // --------------------------------------------------------------------------
  // OnCompletion Listener
  // --------------------------------------------------------------------------

  /**
   * Interface definition for a callback to be invoked when playback of media
   * source has completed.
   */
  public static interface OnCompletionListener {

    public static final int REASON_EOS = 0;
    public static final int REASON_STOPPED = 1;
    public static final int REASON_ERROR = 2;
    public static final int REASON_UNKNOWN = -1;

    /**
     * Called when the end of a media source is reached during playback.
     */
    void onCompletion(int reason);

  }

  private OnCompletionListener onCompletionListener = null;

  private DTalkSubscribeHandle onCompletionDTalkSH = null;
  private final DTalkEventListener onCompletionDTalkEL = new DTalkEventListener() {
    @Override
    public void messageSent(String topic, JSONObject message) {
      onCompletionListener.onCompletion(message.optInt(DTalk.KEY_BODY_PARAMS, OnCompletionListener.REASON_UNKNOWN));
    }
  };

  public OnCompletionListener getOnCompletionListener() {
    return onCompletionListener;
  }

  public void setOnCompletionListener(OnCompletionListener listener) {
    if (onCompletionListener == listener) {
      return;
    }

    if (onCompletionDTalkSH != null) {
      onCompletionDTalkSH.remove();
      onCompletionDTalkSH = null;
    }

    onCompletionListener = listener;

    if (onCompletionListener != null) {
      onCompletionDTalkSH = DTalk.subscribe(eventCallbackService("oncompletion"), onCompletionDTalkEL);
    }
  }

  // --------------------------------------------------------------------------
  // OnError Listener
  // --------------------------------------------------------------------------

  /**
   * Interface definition of a callback to be invoked when there has been an
   * error during an asynchronous operation (other errors will throw exceptions
   * at method call time).
   */
  public static interface OnErrorListener {

    /**
     * Called to indicate an error.
     * 
     * @param error the type of error that has occurred
     */
    void onError(JSONObject error);

  }

  private OnErrorListener onErrorListener = null;

  private DTalkSubscribeHandle onErrorDTalkSH = null;
  private final DTalkEventListener onErrorDTalkEL = new DTalkEventListener() {
    @Override
    public void messageSent(String topic, JSONObject message) {
      onErrorListener.onError(message.optJSONObject(DTalk.KEY_BODY_PARAMS));
    }
  };

  public OnErrorListener getOnErrorListener() {
    return onErrorListener;
  }

  public void setOnErrorListener(OnErrorListener listener) {
    if (onErrorListener == listener) {
      return;
    }

    if (onErrorDTalkSH != null) {
      onErrorDTalkSH.remove();
      onErrorDTalkSH = null;
    }

    onErrorListener = listener;

    if (onErrorListener != null) {
      onErrorDTalkSH = DTalk.subscribe(eventCallbackService("onerror"), onErrorDTalkEL);
    }
  }

  // --------------------------------------------------------------------------
  // OnPrepared Listener
  // --------------------------------------------------------------------------

  /**
   * 
   */
  public static interface OnPreparedListener {

    /**
     * 
     */
    void onPrepared();

  }

  private OnPreparedListener onPreparedListener = null;

  private DTalkSubscribeHandle onPreparedDTalkSH = null;
  private final DTalkEventListener onPreparedDTalkEL = new DTalkEventListener() {
    @Override
    public void messageSent(String topic, JSONObject message) {
      onPreparedListener.onPrepared();
    }
  };

  public OnPreparedListener getOnPreparedListener() {
    return onPreparedListener;
  }

  public void setOnPreparedListener(OnPreparedListener listener) {
    if (onPreparedListener == listener) {
      return;
    }

    if (onPreparedDTalkSH != null) {
      onPreparedDTalkSH.remove();
      onPreparedDTalkSH = null;
    }

    onPreparedListener = listener;

    if (onPreparedListener != null) {
      onPreparedDTalkSH = DTalk.subscribe(eventCallbackService("onprepared"), onPreparedDTalkEL);
    }
  }

  // --------------------------------------------------------------------------
  // OnStatus Listener
  // --------------------------------------------------------------------------

  /**
   * 
   */
  public static interface OnStatusListener {

    /**
     * 
     */
    void onStatus(JSONObject status);

  }

  private OnStatusListener onStatusListener = null;

  private DTalkSubscribeHandle onStatusDTalkSH = null;
  private final DTalkEventListener onStatusDTalkEL = new DTalkEventListener() {
    @Override
    public void messageSent(String topic, JSONObject message) {
      onStatusListener.onStatus(message.optJSONObject(DTalk.KEY_BODY_PARAMS));
    }
  };

  public OnStatusListener getOnStatusListener() {
    return onStatusListener;
  }

  public void setOnStatusListener(OnStatusListener listener) {
    if (this.onStatusListener == listener) {
      return;
    }

    if (onStatusDTalkSH != null) {
      onStatusDTalkSH.remove();
      onStatusDTalkSH = null;
    }

    onStatusListener = listener;

    if (onStatusListener != null) {
      onStatusDTalkSH = DTalk.subscribe(eventCallbackService("onstatus"), onStatusDTalkEL);
    }
  }

}

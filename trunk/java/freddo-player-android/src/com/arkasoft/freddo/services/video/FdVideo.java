package com.arkasoft.freddo.services.video;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arkasoft.freddo.FdActivity;
import com.arkasoft.freddo.R;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import freddo.dtalk.AsyncCallback;
import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkEventListener;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.DTalkSubscribeHandle;
import freddo.dtalk.events.MediaRouterEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdVideo extends  FdService {
  private static final String TAG = LOG.tag(FdVideo.class);

  public static final String TYPE = SRV_PREFIX + "Video";

  private static final long MONITOR_INTERVAL = 1000L; // every 1sec

  private VideoView mVideoView;
  private ImageView mImageView;

  private String mSrc;
  private double mStartTimePercent = 0D;
  private JSONObject mItem = null;

  private double seekToOnPrepared = 0D;

  private Handler mStatusHandler;
  private Runnable mStatusRunnable;

  private String mCastProtocol = null;

  private DTalkSubscribeHandle mCastOnPreparedSH = null;
  private DTalkSubscribeHandle mCastOnCompletionSH = null;
  private DTalkSubscribeHandle mCastOnErrorSH = null;

  private PlaybackState mPlaybackState = PlaybackState.IDLE;
  private boolean mPaused = false;

  // List of various states that we can be in
  static enum PlaybackState {
    PLAYING, BUFFERING, IDLE
  }

  public FdVideo(DTalkServiceContext activity, JSONObject options) {
    super(activity, TYPE, options);

    mVideoView = (VideoView) ((FdActivity)getContext()).findViewById(R.id.videoView);
    mImageView = (ImageView) ((FdActivity)getContext()).findViewById(R.id.imageView);

    mVideoView.setOnCompletionListener(new OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        FdVideo.this.onCompletion();
      }
    });

    mVideoView.setOnErrorListener(new OnErrorListener() {
      @Override
      public boolean onError(MediaPlayer mp, int what, int extra) {
        FdVideo.this.onError(what, extra);
        return true;
      }
    });

    mVideoView.setOnPreparedListener(new OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        FdVideo.this.onPrepared();
      }
    });

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setVideoViewVisibility(View.INVISIBLE);
        setImageViewVisibility(View.INVISIBLE);

        mStatusHandler = new Handler();
        mStatusRunnable = new Runnable() {
          @Override
          public void run() {
            if (mPlaybackState != PlaybackState.IDLE) {
              if (mCastProtocol != null) {
                DTalk.sendGet(null, mCastProtocol, "info", new AsyncCallback<JSONObject>() {
                  @Override
                  public void onFailure(Throwable cause) {
                    LOG.e(TAG, cause.getMessage());
                  }

                  @Override
                  public void onSuccess(JSONObject info) {
                    fireEvent("onstatus", info);
                  }
                });
              } else {
                fireEvent("onstatus", getInfo());
              }
            }

            if (!isDisposed()) {
              mStatusHandler.postDelayed(mStatusRunnable, MONITOR_INTERVAL);
            }
          }
        };
        mStatusHandler.postDelayed(mStatusRunnable, 0L); // start now
      }
    });
  }

  protected void onPrepared() {
    LOG.v(TAG, ">>> onPrepared");

    mPlaybackState = PlaybackState.PLAYING;
    // mPrepared = true;

    if (mStartTimePercent > 0 && mStartTimePercent < 1) {
      LOG.d(TAG, "Seek to startTimePercent: %f", mStartTimePercent);
      seekTo(mStartTimePercent * Double.valueOf(mVideoView.getDuration() / 1000D));
      mStartTimePercent = 0D;
    } else if (seekToOnPrepared > 0D) {
      seekTo(seekToOnPrepared);
      seekToOnPrepared = 0D;
    }

    fireEvent("onprepared");

    ((FdActivity)getContext()).spinnerStop();
  }

  protected void onCompletion() {
    LOG.v(TAG, ">>> onCompletion");

    mPlaybackState = PlaybackState.IDLE;
    // mPrepared = false;

    fireEvent("oncompletion");

    setVideoViewVisibility(View.INVISIBLE);
  }

  protected void onError(int what, int extra) {
    LOG.v(TAG, ">>> onError: %d,%d", what, extra);

    mPlaybackState = PlaybackState.IDLE;
    // mPrepared = false;

    String msg = "";
    if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
      msg = ((FdActivity)getContext()).getString(R.string.video_error_media_load_timeout);
    } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      msg = ((FdActivity)getContext()).getString(R.string.video_error_server_unaccessible);
    } else {
      msg = ((FdActivity)getContext()).getString(R.string.video_error_unknown_error);
    }

    JSONObject error = new JSONObject();
    try {
      error.put("message", msg);
      error.put("what", what);
      error.put("extra", extra);
    } catch (JSONException e) {
      // Ignore
    }
    fireEvent("onerror", error);

    setVideoViewVisibility(View.INVISIBLE);

    Toast.makeText(((FdActivity)getContext()), msg, Toast.LENGTH_SHORT).show();
    ((FdActivity)getContext()).spinnerStop();
  }

  private void setVideoViewVisibility(int visibility) {
    ViewGroup parent = (ViewGroup) mVideoView.getParent();
    parent.setVisibility((visibility == View.VISIBLE || mImageView.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.INVISIBLE);
    mVideoView.setVisibility(visibility);
  }

  private void setImageViewVisibility(int visibility) {
    ViewGroup parent = (ViewGroup) mVideoView.getParent();
    parent.setVisibility((mVideoView.getVisibility() == View.VISIBLE || visibility == View.VISIBLE) ? View.VISIBLE : View.INVISIBLE);
    mImageView.setVisibility(visibility);
  }

  public void getSrc(JSONObject request) {
    sendResponse(request, getSrc());
  }

  private String getSrc() {
    // LOG.v(TAG, ">>> getSrc");

    return mSrc;
  }

  public void getInfo(JSONObject request) throws DTalkException {
    if (mCastProtocol != null) {
      DTalk.forward(mCastProtocol, request);
    } else {
      sendResponse(request, getInfo());
    }
  }

  private JSONObject getInfo() {
    JSONObject info = new JSONObject();

    try {
      info.put("src", mSrc); // XXX remove it
      info.put("volume", getVolume());

      info.put("canPause", mVideoView.canPause());

      // false if the player is currently playing, or true otherwise
      info.put("paused", isPaused());

      info.put("position", Double.valueOf(mVideoView.getCurrentPosition() / 1000D));

      // The duration of the video in seconds.
      // The video must have started loading before the duration can be known.
      int videoDuration = mVideoView.getDuration();
      if (videoDuration <= 0) {
        info.put("duration", Double.valueOf(0.0D));
      } else {
        info.put("duration", Double.valueOf(mVideoView.getDuration() / 1000D));
      }

      // The percent (as a decimal) of the video that's been downloaded:
      // 0 means none, 1 means all.
      info.put("bufferedPercent", mVideoView.getBufferPercentage() / 100D);

    } catch (JSONException e) {
      // Ignore
    }
    return info;
  }

  public void getVolume(JSONObject request) {
    sendResponse(request, getVolume());
  }

  private Object getVolume() {
    // LOG.v(TAG, ">>> getVolume");

    AudioManager audioManager = (AudioManager) ((FdActivity)getContext()).getSystemService(Context.AUDIO_SERVICE);
    return (double) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        / (double) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
  }

  public void getItem(JSONObject request) {
    sendResponse(request, getItem());
  }

  private static JSONObject sItem = null;

  private JSONObject getItem() {
    LOG.v(TAG, ">>> getItem");

    if (mItem == null) {
      if (sItem == null) {
        sItem = new JSONObject();
      }
      if (mSrc != null) {
        try {
          sItem.put("src", mSrc);
        } catch (JSONException e) {
          // ignore
        }
      } else {
        sItem.remove("src");
      }
      mItem = sItem;
    }
    return mItem;
  }

  public void setSrc(JSONObject options) {
    String src = getString(options, "src");
    if (src != null) {
      setSrc(src);
    }
  }

  public void setStartPositionPercent(JSONObject options) {
    double value = getDouble(options, "startPositionPercent");
    if (!Double.isNaN(value)) {
      setStartTimePercent(value);
    }
  }

  public void setItem(JSONObject options) {
    JSONObject value = getJSONObject(options, "item");
    if (value != null) {
      setItemImpl(value);
    }
  }

  public void setVolume(JSONObject options) {
    double value = getDouble(options, "volume");
    if (!Double.isNaN(value)) {
      setVolume(value);
    }
  }

  private void setSrc(String src) {
    LOG.v(TAG, ">>> setSrc: %s", src);

    if (mImageView.getVisibility() == View.VISIBLE) {
      setImageViewVisibility(View.INVISIBLE);
    }

    if (mVideoView.getVisibility() != View.VISIBLE) {
      setVideoViewVisibility(View.VISIBLE);
    }

    mPlaybackState = PlaybackState.BUFFERING;
    // mPrepared = false;

    mSrc = src;
    mItem = null; // clear item

    if (mCastProtocol != null) {
      LOG.d(TAG, "mCastProtocol: %s", mCastProtocol);

      try {
        DTalk.sendSet(null, mCastProtocol, "src", mSrc);
      } catch (Exception e) {
        LOG.e(TAG, e.getMessage());
      }

    } else {
      mVideoView.setVideoPath(mSrc);
    }
  }

  private void setStartTimePercent(double startTimePercent) {
    LOG.v(TAG, ">>> setStartTimePercent: %f", startTimePercent);
    mStartTimePercent = startTimePercent;
  }

  private void setItemImpl(JSONObject item) {
    LOG.v(TAG, ">>> setItem: %s", item);

    if (item.has("src")) {
      // NOTE: setSrc will clear this.item; we set this.item
      // after setSrc() call...
      setSrc(item.optString("src"));
      this.mItem = item;
    }
  }

  private void setVolume(double value) {
    LOG.v(TAG, ">>> setVolume: %f", value);

    if (value >= 0D && value <= 1D) {
      AudioManager audioManager = (AudioManager) ((FdActivity)getContext()).getSystemService(Context.AUDIO_SERVICE);
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
          (int) (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * value),
          AudioManager.FLAG_SHOW_UI);
    }
  }

  private boolean isPaused() {
    return mPaused && mPlaybackState != PlaybackState.IDLE;
  }

  public void doPause(JSONObject message) {
    LOG.v(TAG, ">>> doPause");
    
    mPaused = true;

    if (mCastProtocol != null) {
      MessageBus.sendMessage(mCastProtocol, message);
    } else {
      mVideoView.pause();
    }
  }

  public void doSeekTo(JSONObject message) {
    double sec = message.optDouble(MessageEvent.KEY_BODY_PARAMS, Double.NaN);
    LOG.v(TAG, ">>> doSeekTo: %f", sec);
    
    if (mCastProtocol != null) {
      try {
        DTalk.forward(mCastProtocol, message);
      } catch (DTalkException e) {
        LOG.e(TAG, e.getMessage());
      }
    } else {
      if (!Double.isNaN(sec) && sec > 0D) {
        seekTo(sec);
      }
    }
  }

  private void seekTo(double sec) {
    // if (videoView.canSeekBackward() || videoView.canSeekForward()) {
    if (mPlaybackState == PlaybackState.PLAYING) {
      // if (mPrepared) {
      final int msec = (int) (1000 * sec);
      mVideoView.seekTo(msec);
    } else {
      seekToOnPrepared = sec;
    }
    // }
  }

  public void doPlay(JSONObject message) {
    LOG.v(TAG, ">>> doPlay");

    if (mImageView.getVisibility() == View.VISIBLE) {
      setImageViewVisibility(View.INVISIBLE);
    }

    if (mVideoView.getVisibility() != View.VISIBLE) {
      setVideoViewVisibility(View.VISIBLE);
    }
    
    mPaused = false;

    if (mCastProtocol != null) {
      try {
        DTalk.forward(mCastProtocol, message);
      } catch (DTalkException e) {
        LOG.e(TAG, e.getMessage());
      }
    } else {
      mVideoView.start();
    }

    if (mPlaybackState == PlaybackState.BUFFERING) {
      ((FdActivity)getContext()).spinnerStart();
    }
  }

  public void doStop(JSONObject message) {
    LOG.v(TAG, ">>> stop");

    if (mCastProtocol != null) {
      try {
        DTalk.forward(mCastProtocol, message);
      } catch (DTalkException e) {
        LOG.e(TAG, e.getMessage());
      }
    } else {
      mVideoView.stopPlayback();
    }

    mPlaybackState = PlaybackState.IDLE;
    mPaused = false;

    fireEvent("oncompletion");

    setVideoViewVisibility(View.INVISIBLE);
  }

  @Deprecated
  public void doSetRate(JSONObject message) {
    double rate = message.optDouble(MessageEvent.KEY_BODY_PARAMS, 1D);
    LOG.v(TAG, "setRate: %f", rate);
    if (!Double.isNaN(rate)) {
      if (rate == 0D) {
        doPause(PAUSE());
      } else {
        doPlay(PLAY());
      }
    }
  }

  public void setPhoto(JSONObject options) {
    String src = getString(options, "photo");
    LOG.v(TAG, ">>> setPhoto: %s", src);
    if (src != null) {
      setPhoto(src);
    }
  }

  private void setPhoto(String url) {
    LOG.v(TAG, ">>> photo: %s", url);

    if (mImageView.getVisibility() != View.VISIBLE) {
      setImageViewVisibility(View.VISIBLE);
    }

    UrlImageViewHelper.setUrlDrawable(mImageView, url, R.drawable.image_placeholder, 0L, new UrlImageViewCallback() {
      private final String TAG = LOG.tag(UrlImageViewCallback.class);

      @Override
      public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
        LOG.v(TAG, ">>> onLoad: %s (%b)", url, loadedFromCache);
        fireEvent("onload", loadedBitmap != null); // Breaks code in clients...
      }
    });
  }

  @Override
  protected void start() {
    LOG.v(TAG, ">>> start");

    MessageBus.subscribe(MediaRouterEvent.class.getName(), mMediaRouterEH);
  }

  @Override
  protected void reset() {
    LOG.v(TAG, ">>> reset");

    // runOnUiThread(new Runnable() {
    // @Override
    // public void run() {
    // stop();
    // }
    // });

    mVideoView.setOnPreparedListener(null);
    mVideoView.setOnCompletionListener(null);
    mVideoView.setOnErrorListener(null);

    MessageBus.unsubscribe(MediaRouterEvent.class.getName(), mMediaRouterEH);
  }

  // --------------------------------------------------------------------------
  // CAST SUPPORT
  // --------------------------------------------------------------------------

  private Map<String, JSONObject> mCastDevices = null;

  private final MessageBusListener<MediaRouterEvent> mMediaRouterEH = new MessageBusListener<MediaRouterEvent>() {
    @Override
    public void messageSent(String topic, MediaRouterEvent message) {
      LOG.v(TAG, ">>> mesageSent: %s", message);
      switch (message.getType()) {
        case ADDED:
          onRouteAdded(message.getProtocol(), message.getId(), message.getName());
          break;
        case REMOVED:
          onRouteRemoved(message.getProtocol(), message.getId());
          break;
        default:
          LOG.w(TAG, "Unsupported event type: %s", message.getType());
          break;
      }
    }
  };

  protected void onRouteAdded(String protocol, String id, String name) {
    LOG.v(TAG, ">>> onRouteAdded: %s", id);

    if (mCastDevices == null) {
      mCastDevices = new ConcurrentHashMap<String, JSONObject>();
    }

    try {
      JSONObject castDevice = new JSONObject();
      castDevice.put("protocol", protocol);
      castDevice.put("id", id);
      castDevice.put("name", name);
      mCastDevices.put(id, castDevice);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  protected void onRouteRemoved(String protocol, String id) {
    LOG.v(TAG, ">>> onRouteRemoved: %s", id);

    if (mCastDevices != null) {
      mCastDevices.remove(id);
    }
  }

  public void getCastDevices(JSONObject request) {
    sendResponse(request, getCastDevices());
  }

  private JSONArray getCastDevices() {
    JSONArray array = new JSONArray();
    if (mCastDevices != null) {
      for (JSONObject castDevice : mCastDevices.values()) {
        array.put(castDevice);
      }
    }
    return array;
  }

  public void doCast(JSONObject request) {
    final String id = request.optString(DTalk.KEY_BODY_PARAMS, null);
    if (id != null) {
      final JSONObject castDevice = mCastDevices.get(id);
      if (castDevice != null) {
        final String protocol = castDevice.optString("protocol", null);
        if (protocol != null) {
          //mCastCurrentPlayback = false;
          if (mCastProtocol != null) {
            // TODO check previous protocol...
          } else if (mPlaybackState != PlaybackState.IDLE) {
            //mCastCurrentPlayback = true;
          }
          mCastProtocol = protocol;
          LOG.d(TAG, "mCastProtocol: %s", mCastProtocol);
          try {
            DTalk.forward(mCastProtocol, request);
          } catch (DTalkException e) {
            LOG.e(TAG, e.getMessage());
          }
        } else {
          sendErrorResponse(request, "Cast protocol not defined");
        }
      } else {
        sendErrorResponse(request, "Cast device not found");
      }
    } else if (mCastProtocol != null) {
      final String protocol = mCastProtocol;
      mCastProtocol = null;
      try {
        DTalk.forward(protocol, request);
      } catch (DTalkException e) {
        LOG.e(TAG, e.getMessage());
      }
    } else {
      sendErrorResponse(request, "Invalid request");
    }

    if (mCastProtocol != null) {
      removeCastHandlers();

      mCastOnPreparedSH = DTalk.subscribe(CAST_EVENT(mCastProtocol, "onprepared"), new DTalkEventListener() {
        @Override
        public void messageSent(String arg0, JSONObject arg1) {
          FdVideo.this.onPrepared();
        }
      });

      mCastOnCompletionSH = DTalk.subscribe(CAST_EVENT(mCastProtocol, "oncompletion"), new DTalkEventListener() {
        @Override
        public void messageSent(String arg0, JSONObject arg1) {
          FdVideo.this.onCompletion();
        }
      });

      mCastOnErrorSH = DTalk.subscribe(CAST_EVENT(mCastProtocol, "onerror"), new DTalkEventListener() {
        @Override
        public void messageSent(String arg0, JSONObject arg1) {
          FdVideo.this.onError(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_UNKNOWN);
        }
      });
    }
  }

  private void removeCastHandlers() {
    if (mCastOnPreparedSH != null) {
      mCastOnPreparedSH.remove();
      mCastOnPreparedSH = null;
    }
    if (mCastOnCompletionSH != null) {
      mCastOnCompletionSH.remove();
      mCastOnCompletionSH = null;
    }
    if (mCastOnErrorSH != null) {
      mCastOnErrorSH.remove();
      mCastOnErrorSH = null;
    }
  }

  // --------------------------------------------------------------------------
  // Utility commands
  // --------------------------------------------------------------------------

  private static String CAST_EVENT(String castProtocol, String event) {
    return String.format("$%s.%s", castProtocol, event);
  }

  private static JSONObject sPauseCmd = null;

  private static JSONObject PAUSE() {
    if (sPauseCmd == null) {
      try {
        sPauseCmd = DTalk.createMessage(TYPE, "pause");
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return sPauseCmd;
  }

  private static JSONObject sPlayCmd = null;

  private static JSONObject PLAY() {
    if (sPlayCmd == null) {
      try {
        sPlayCmd = DTalk.createMessage(TYPE, "play");
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return sPlayCmd;
  }

}

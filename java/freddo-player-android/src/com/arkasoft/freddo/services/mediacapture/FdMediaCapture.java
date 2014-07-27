package com.arkasoft.freddo.services.mediacapture;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import com.arkasoft.freddo.FdActivity;
import com.arkasoft.freddo.FdActivityResultCallback;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

/**
 * This class launches the camera view, allows the user to take a picture,
 * closes the camera view, and returns the captured image.
 */
public class FdMediaCapture extends FdService implements FdActivityResultCallback {
  private static final String TAG = LOG.tag(FdMediaCapture.class);

  public static final String TYPE = SRV_PREFIX + "MediaCapture";

  private static final String VIDEO_3GPP = "video/3gpp";
  private static final String VIDEO_MP4 = "video/mp4";
  private static final String AUDIO_3GPP = "audio/3gpp";
  private static final String IMAGE_JPEG = "image/jpeg";

  private static final int CAPTURE_AUDIO = 0; // Constant for capture audio
  private static final int CAPTURE_IMAGE = 1; // Constant for capture image
  private static final int CAPTURE_VIDEO = 2; // Constant for capture video

  private static final int CAPTURE_INTERNAL_ERR = 0;
  // private static final int CAPTURE_APPLICATION_BUSY = 1;
  // private static final int CAPTURE_INVALID_ARGUMENT = 2;
  private static final int CAPTURE_NO_MEDIA_FILES = 3;

  protected FdMediaCapture(DTalkServiceContext activity, JSONObject options) {
    super(activity, TYPE, options);
  }

  public void doCaptureAudio(JSONObject message) {
    LOG.v(TAG, ">>> doCaptureAudio");
    Intent intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    ((FdActivity)getContext()).startActivityForResult(this, intent, CAPTURE_AUDIO);
  }

  public void doCaptureImage(JSONObject message) {
    LOG.v(TAG, ">>> doCaptureImage");
    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
 
    // Specify file so that large image is captured and returned
    //File photo = new File(DirectoryManager.getTempDirectoryPath(((FdActivity)getContext())), "Capture.jpg");
    //intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
    
    ((FdActivity)getContext()).startActivityForResult(this, intent, CAPTURE_IMAGE);
  }

  public void doCaptureVideo(JSONObject message) {
    LOG.v(TAG, ">>> doCaptureVideo");
    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    // intent.putExtra("android.intent.extra.durationLimit", duration);
    ((FdActivity)getContext()).startActivityForResult(this, intent, CAPTURE_VIDEO);
  }

  /**
   * Called when the video view exits.
   * 
   * @param requestCode The request code originally supplied to
   *          startActivityForResult(), allowing you to identify who this result
   *          came from.
   * @param resultCode The integer result code returned by the child activity
   *          through its setResult().
   * @param data An Intent, which can return result data to the caller (various
   *          data can be attached to Intent "extras").
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, final Intent data) {
    LOG.v(TAG, "onActivityResult (requestCode=%d, resultCode=%d)", requestCode, resultCode);
    
    if(resultCode == Activity.RESULT_OK) {
      // And audio clip was requested
      switch (requestCode) {
        case CAPTURE_AUDIO:
          break;
        case CAPTURE_IMAGE:
          break;
        case CAPTURE_VIDEO:
          Runnable captureVideo = new Runnable() {
            @Override
            public void run() {
              // Get the uri of the video clip
              Uri _data = data.getData();
              if (_data == null) {
                
              } else {
                
              }
            }
          };
          
          break;
      }
    }
  }

  @Override
  protected void reset() {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void start() {
    // TODO Auto-generated method stub
    
  }

}

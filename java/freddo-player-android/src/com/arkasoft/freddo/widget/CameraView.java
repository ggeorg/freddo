package com.arkasoft.freddo.widget;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import freddo.dtalk.util.LOG;

public class CameraView extends View implements SurfaceHolder.Callback {
  private static final String TAG = LOG.tag(CameraView.class);

  private AudioManager mAudioManager = null;
  private Camera mCamera = null;
  private MediaRecorder mMediaRecorder = null;
  private SurfaceHolder mCamSHolder;
  private SurfaceView mCameraSView;

  public CameraView(Context c, AttributeSet attr) {
    super(c, attr);

    mAudioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
  }

  public void SetupCamera(SurfaceView sv) {
    LOG.v(TAG, ">>> setupCamera");
    
    mCameraSView = sv;
    mCamSHolder = mCameraSView.getHolder();
    mCamSHolder.addCallback(this);
    mCamSHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    mCamera = Camera.open();
  }

  public void prepareMedia(int wid, int hei) {
    LOG.v(TAG, ">>> PrepareMedia");
    
    mMediaRecorder = new MediaRecorder();
    mCamera.stopPreview();
    mCamera.unlock();

    mMediaRecorder.setCamera(mCamera);
    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

    CamcorderProfile targetProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
    targetProfile.videoFrameWidth = wid;
    targetProfile.videoFrameHeight = hei;
    targetProfile.videoFrameRate = 30;
    targetProfile.videoBitRate = 512 * 1024;
    targetProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
    targetProfile.audioCodec = MediaRecorder.AudioEncoder.AMR_NB;
    targetProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
    mMediaRecorder.setProfile(targetProfile);
  }

  private boolean realyStart() {
    LOG.v(TAG, ">>> realyStart");
    
    mMediaRecorder.setPreviewDisplay(mCamSHolder.getSurface());
    try {
      mMediaRecorder.prepare();
    } catch (IllegalStateException e) {
      releaseMediaRecorder();
      LOG.e(TAG, "JAVA:  camera prepare illegal error");
      return false;
    } catch (IOException e) {
      releaseMediaRecorder();
      LOG.e(TAG, "JAVA:  camera prepare io error");
      return false;
    }

    try {
      mMediaRecorder.start();
    } catch (Exception e) {
      releaseMediaRecorder();
      LOG.d(TAG, "JAVA:  camera start error", e);
      return false;
    }

    return true;
  }

  public boolean startStreaming(String targetFile) {
    LOG.v(TAG, ">>> startStreaming");
    
    mMediaRecorder.setOutputFile(targetFile);
    mMediaRecorder.setMaxDuration(9600000); // Set max duration 4 hours
    // myMediaRecorder.setMaxFileSize(1600000000); // Set max file size 16G
    mMediaRecorder.setOnInfoListener(streamingEventHandler);
    return realyStart();
  }

  public boolean startRecording(String targetFile) {
    LOG.v(TAG, ">>> startRecording: %s", targetFile);
    
    mMediaRecorder.setOutputFile(targetFile);
    mMediaRecorder.setMaxDuration(9600000); // Set max duration 4 hours
    mMediaRecorder.setMaxFileSize(1600000000); // Set max file size 16G
    mMediaRecorder.setOnInfoListener(streamingEventHandler);
    return realyStart();
  }

  public void stopMedia() {
    mMediaRecorder.stop();
    releaseMediaRecorder();
  }

  private void releaseMediaRecorder() {
    if (mMediaRecorder != null) {
      mMediaRecorder.reset(); // clear recorder configuration
      mMediaRecorder.release(); // release the recorder object
      mMediaRecorder = null;
      mCamera.lock(); // lock camera for later use
      mCamera.startPreview();
    }
    mMediaRecorder = null;
  }

  private MediaRecorder.OnInfoListener streamingEventHandler = new MediaRecorder.OnInfoListener() {
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
      LOG.d(TAG, "MediaRecorder event = " + what);
    }
  };

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    if (mCamera != null && mMediaRecorder == null) {
      mCamera.stopPreview();
      try {
        mCamera.setPreviewDisplay(holder);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      mCamera.startPreview();
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    // TODO Auto-generated method stub

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    // TODO Auto-generated method stub

  }

}

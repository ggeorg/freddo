package com.arkasoft.freddo.widget;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.VideoView;

public class XVideoView extends FrameLayout {

  private final VideoView videoView;

  public XVideoView(Context context) {
    super(context);

    this.videoView = new VideoView(context);
    LayoutParams params =
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
    this.addView(this.videoView, params);
  }

  public VideoView getVideoView() {
    return videoView;
  }

}

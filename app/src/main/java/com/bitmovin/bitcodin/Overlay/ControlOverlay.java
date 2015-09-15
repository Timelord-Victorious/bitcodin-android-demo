package com.bitmovin.bitcodin.Overlay;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bitmovin.bitcodin.R;
import com.bitmovin.bitcodin.Settings;
import com.bitmovin.bitcodin.Utils;
import com.google.android.exoplayer.VideoSurfaceView;

import java.lang.ref.WeakReference;

@SuppressWarnings("FieldCanBeLocal")
public class ControlOverlay {
  private Context context;
  private RelativeLayout overlayWrapper;

  /* controls */
  private RelativeLayout controlWrapper;
  private ImageView playPauseButton;
  private ProgressBar playerProgressBar;
  private SeekBar playerSeekBar;
  private TextView playbackTime;
  private ImageView changeScalingButton;

  /* tooltip */
  private LinearLayout tooltipWrapper;
  private TextView tooltipText;

  /* title */
  private RelativeLayout titleWrapper;
  private TextView title;

  /* animations */
  private AlphaAnimation fadeInTooltipAnimation;
  private AlphaAnimation fadeOutTooltipAnimation;
  private AlphaAnimation fadeInOverlayAnimation;
  private AlphaAnimation fadeOutOverlayAnimation;

  /* other */
  private MediaPlayerControl mPlayerControl;
  private VideoSurfaceView mVideoSurfaceView;
  private int currentScalingMode;
  private boolean overlayVisible;
  private boolean tooltipVisible;
  private boolean currentlyDragging;
  private int[] playerSeekBarPosition;
  private ProgressHandler mProgressHandler;

  private enum FadingViews {
    OVERLAY,
    TOOLTIP
  }

  public ControlOverlay(final Context context, RelativeLayout overlayWrapper,
                        LinearLayout tooltipWrapper, VideoSurfaceView mVideoSurfaceView) {
    this.context = context;
    this.overlayWrapper = overlayWrapper;
    this.tooltipWrapper = tooltipWrapper;
    this.mVideoSurfaceView = mVideoSurfaceView;
    if (this.overlayWrapper != null) {
      /* controls */
      this.controlWrapper = (RelativeLayout)
          this.overlayWrapper.findViewById(R.id.player_rlt_control_wrapper);
      this.playPauseButton = (ImageView)
          this.controlWrapper.findViewById(R.id.player_iv_control_play_pause);
      this.playerProgressBar = (ProgressBar)
          this.controlWrapper.findViewById(R.id.player_pb_control_progress);
      this.playerSeekBar = (SeekBar)
          this.controlWrapper.findViewById(R.id.player_sb_control_seek);
      this.playbackTime = (TextView)
          this.controlWrapper.findViewById(R.id.player_tv_control_playback_time);
      this.changeScalingButton = (ImageView)
          this.controlWrapper.findViewById(R.id.player_iv_control_change_scaling);

      /* tooltip */
      this.tooltipText = (TextView)
          this.tooltipWrapper.findViewById(R.id.player_tv_tooltip_text);

      /* title */
      this.titleWrapper = (RelativeLayout)
          this.overlayWrapper.findViewById(R.id.player_rlt_title_wrapper);
      this.title = (TextView) this.titleWrapper.findViewById(R.id.player_tv_title);

      this.playerProgressBar.setMax(1000);
      this.playerSeekBar.setMax(1000);

      this.controlWrapper.setOnClickListener(this.controlWrapperClickListener);
      this.playPauseButton.setOnClickListener(this.playPauseClickListener);
      this.playerSeekBar.setOnSeekBarChangeListener(this.playerSeekBarChangeListener);
      this.changeScalingButton.setOnClickListener(this.changeScalingClickListener);

      this.setupAnimations();

      this.currentScalingMode = VideoSurfaceView.ScalingMode.FIT;

      this.overlayVisible = false;
      this.tooltipVisible = false;
      this.currentlyDragging = false;

      this.playerSeekBarPosition = new int[2];
      this.mProgressHandler = new ProgressHandler(this);
    }
  }

  public void setMediaPlayer(MediaPlayerControl mPlayerControl) {
    this.mPlayerControl = mPlayerControl;
    show();
  }

  // region animations and visibility

  private void setupAnimations() {
    this.fadeInOverlayAnimation = new AlphaAnimation(0, 1);
    this.fadeInOverlayAnimation.setDuration(Settings.FADE_IN_DURATION);
    this.fadeInOverlayAnimation.setAnimationListener(this.mAnimationListener);

    this.fadeOutOverlayAnimation = new AlphaAnimation(1, 0);
    this.fadeOutOverlayAnimation.setDuration(Settings.FADE_OUT_DURATION);
    this.fadeOutOverlayAnimation.setAnimationListener(this.mAnimationListener);

    this.fadeInTooltipAnimation = new AlphaAnimation(0, 1);
    this.fadeInTooltipAnimation.setDuration(Settings.FADE_IN_DURATION);
    this.fadeInTooltipAnimation.setAnimationListener(this.mAnimationListener);

    this.fadeOutTooltipAnimation = new AlphaAnimation(1, 0);
    this.fadeOutTooltipAnimation.setDuration(Settings.FADE_OUT_DURATION);
    this.fadeOutTooltipAnimation.setAnimationListener(this.mAnimationListener);
  }

  private Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
    @Override
    public void onAnimationStart(Animation animation) {
      if (animation == fadeInOverlayAnimation) {
        overlayWrapper.setVisibility(View.VISIBLE);
        overlayVisible = true;
      } else if (animation == fadeInTooltipAnimation) {
        tooltipWrapper.setVisibility(View.VISIBLE);
        updateTooltipPosition();
        tooltipVisible = true;
      } else if (animation == fadeOutOverlayAnimation) {
        overlayVisible = false;
      } else if (animation == fadeOutTooltipAnimation) {
        tooltipVisible = false;
      }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
      if (animation == fadeOutOverlayAnimation) {
        overlayWrapper.setVisibility(View.GONE);
        overlayVisible = false;
      } else if (animation == fadeOutTooltipAnimation) {
        tooltipWrapper.setVisibility(View.GONE);
        tooltipVisible = false;
      }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
  };

  public void show() {
    setProgress();
    updatePlayPause();
    fadeView(true, FadingViews.OVERLAY);
    this.mProgressHandler.sendEmptyMessage(ProgressHandler.Actions.SHOW_PROGRESS);
    this.mProgressHandler.removeMessages(ProgressHandler.Actions.FADE_OUT);
    this.mProgressHandler.sendMessageDelayed(
        this.mProgressHandler.obtainMessage(ProgressHandler.Actions.FADE_OUT),
        Settings.FADE_OUT_TIMEOUT
    );
  }

  public void hide() {
    try {
      if (this.currentlyDragging || this.mPlayerControl.hasEnded()) {
        this.mProgressHandler.removeMessages(ProgressHandler.Actions.FADE_OUT);
        this.mProgressHandler.sendMessageDelayed(
            this.mProgressHandler.obtainMessage(ProgressHandler.Actions.FADE_OUT),
            Settings.FADE_OUT_TIMEOUT
        );
      } else {
        fadeView(false, FadingViews.OVERLAY);
      }
    } catch (IllegalArgumentException ex) {
      Log.w("MediaController", "already removed");
    }
  }

  private void fadeView(boolean fadeIn, FadingViews view) {
    Animation mAnimation;
    switch (view) {

      case OVERLAY:
        if (!(this.overlayVisible ^ fadeIn)) break;
        mAnimation = fadeIn ? this.fadeInOverlayAnimation : this.fadeOutOverlayAnimation;
        this.overlayWrapper.clearAnimation();
        mAnimation.reset();
        this.overlayWrapper.setAnimation(mAnimation);
        mAnimation.start();
        break;

      case TOOLTIP:
        if (!(this.tooltipVisible ^ fadeIn)) break;
        mAnimation = fadeIn ? this.fadeInTooltipAnimation : this.fadeOutTooltipAnimation;
        this.tooltipWrapper.clearAnimation();
        mAnimation.reset();
        this.tooltipWrapper.setAnimation(mAnimation);
        mAnimation.start();
        break;
    }
  }

  // endregion animations and visibility

  // region click listeners

  private View.OnClickListener changeScalingClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      show();
      switch (currentScalingMode) {
        case VideoSurfaceView.ScalingMode.FIT:
          currentScalingMode = VideoSurfaceView.ScalingMode.CROP;
          changeScalingButton.setImageResource(R.drawable.scaling_crop);
          break;
        case VideoSurfaceView.ScalingMode.CROP:
          currentScalingMode = VideoSurfaceView.ScalingMode.STRETCH;
          changeScalingButton.setImageResource(R.drawable.scaling_stretch);
          break;
        case VideoSurfaceView.ScalingMode.STRETCH:
          currentScalingMode = VideoSurfaceView.ScalingMode.FIT;
          changeScalingButton.setImageResource(R.drawable.scaling_fit);
          break;
      }
      if (mVideoSurfaceView != null) {
        mVideoSurfaceView.setScalingMode(currentScalingMode);
      }
    }
  };

  private View.OnClickListener playPauseClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      doPauseResume();
      show();
    }
  };

  private View.OnClickListener controlWrapperClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      show();
    }
  };

  private SeekBar.OnSeekBarChangeListener playerSeekBarChangeListener =
      new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
          if (fromUser) {
            long duration = mPlayerControl.getDuration();
            long newPosition = (duration * progress) / 1000L;
            mPlayerControl.seekTo((int) newPosition);
            playerProgressBar.setProgress(progress);
            tooltipText.setText(timeToString((int) newPosition));
            updateTooltipPosition();
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
          show();
          fadeView(true, FadingViews.TOOLTIP);
          currentlyDragging = true;
          mProgressHandler.removeMessages(ProgressHandler.Actions.SHOW_PROGRESS);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
          currentlyDragging = false;
          setProgress();
          updatePlayPause();
          show();
          fadeView(false, FadingViews.TOOLTIP);
          mProgressHandler.sendEmptyMessage(ProgressHandler.Actions.SHOW_PROGRESS);
        }
      };

  // endregion click listeners

  private void updatePlayPause() {
    final int resId = (this.mPlayerControl.isPlaying() &&
        !this.mPlayerControl.hasEnded() &&
        !this.mPlayerControl.isIdling()) ?
        R.drawable.pause : R.drawable.play;
    this.playPauseButton.post(new Runnable() {
      @Override
      public void run() {
        playPauseButton.setImageResource(resId);
      }
    });
  }

  private void doPauseResume() {
    if (this.mPlayerControl.isPlaying()) {
      this.mPlayerControl.pause();
    } else {
      if (this.mPlayerControl.hasEnded()) {
        this.mPlayerControl.seekTo(0);
      }
      this.mPlayerControl.start();
    }
    updatePlayPause();
  }

  private int setProgress() {
    if (this.mPlayerControl == null || this.currentlyDragging) {
      return 0;
    }
    final int position = this.mPlayerControl.getCurrentPosition();
    int duration = this.mPlayerControl.getDuration();
    if (this.playerProgressBar != null) {
      if (duration > 0) {
        this.playerProgressBar.setProgress((int) (1000L * position / duration));
        this.playerSeekBar.setProgress((int) (1000L * position / duration));
      }
      this.playerProgressBar.setSecondaryProgress(this.mPlayerControl.getBufferPercentage() * 10);
    }
    this.playbackTime.setText(timeToString(position) + " / " + timeToString(duration));
    updatePlayPause();
    return position;
  }

  private String timeToString(int timeMs) {
    int totalSeconds = timeMs / 1000;

    int seconds = totalSeconds % 60;
    int minutes = (totalSeconds / 60) % 60;
    int hours = totalSeconds / 3600;

    if (hours > 0) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format("%02d:%02d", minutes, seconds);
    }
  }

  private void updateTooltipPosition() {
    this.playerSeekBar.getLocationOnScreen(this.playerSeekBarPosition);

    this.tooltipWrapper.setY(
        this.playerSeekBarPosition[1] -
            this.tooltipWrapper.getHeight() +
            Utils.toPx(this.context, 17)
    );

    this.tooltipWrapper.setX(
        this.playerSeekBarPosition[0] +
            Utils.toPx(this.context, 5) +
            (int) (
                ((float) this.playerSeekBar.getProgress() /
                    (float) this.playerSeekBar.getMax()) *
                    ((float) this.playerSeekBar.getWidth() -
                        Utils.toPx(this.context, 10))
            ) - (this.tooltipWrapper.getWidth() / 2)
    );
  }

  public void setTitle(final String titleStr) {
    this.title.post(new Runnable() {
      @Override
      public void run() {
        title.setText(titleStr);
      }
    });
  }

  public void pause() {
    if (this.mPlayerControl != null && this.mPlayerControl.isPlaying()) {
      this.mPlayerControl.pause();
    }
  }

  public void play() {
    if (this.mPlayerControl != null && !this.mPlayerControl.hasEnded()) {
      this.mPlayerControl.start();
    }
  }

  @SuppressWarnings("unused")
  public interface MediaPlayerControl {
    void start();

    void pause();

    int getDuration();

    int getCurrentPosition();

    void seekTo(int pos);

    boolean isPlaying();

    boolean hasEnded();

    boolean isIdling();

    int getBufferPercentage();

    boolean canPause();

    boolean canSeekBackward();

    boolean canSeekForward();

    /**
     * Get the audio session id for the player used by this VideoView. This can be used to
     * apply audio effects to the audio track of a video.
     *
     * @return The audio session, or 0 if there was an error.
     */
    int getAudioSessionId();
  }

  private static class ProgressHandler extends Handler {
    private WeakReference<ControlOverlay> mControlOverlayRef;

    public ProgressHandler(ControlOverlay mControlOverlay) {
      this.mControlOverlayRef = new WeakReference<>(mControlOverlay);
    }

    @Override
    public void handleMessage(Message msg) {
      ControlOverlay mControlOverlay = this.mControlOverlayRef.get();
      switch (msg.what) {
        case Actions.FADE_OUT:
          mControlOverlay.hide();
          break;

        case Actions.SHOW_PROGRESS:
          mControlOverlay.setProgress();
          if (!mControlOverlay.currentlyDragging && mControlOverlay.mPlayerControl.isPlaying()) {
            sendMessageDelayed(obtainMessage(Actions.SHOW_PROGRESS), Settings.CONTROL_UPDATE_RATE);
          }
          break;
      }
    }

    public final class Actions {
      public static final int FADE_OUT = 0;
      public static final int SHOW_PROGRESS = 1;
    }
  }
}
package com.bitmovin.bitcodin;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bitmovin.bitcodin.Job.BitcodinJob;
import com.bitmovin.bitcodin.Job.JobManager;
import com.bitmovin.bitcodin.Overlay.BufferingOverlay;
import com.bitmovin.bitcodin.Overlay.ControlOverlay;
import com.bitmovin.bitcodin.Overlay.SidebarOverlay;
import com.bitmovin.bitcodin.Overlay.SidebarOverlayListener;
import com.bitmovin.bitcodin.Player.DemoPlayer;
import com.bitmovin.bitcodin.Player.WidevineTestMediaDrmCallback;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.util.Util;

@SuppressWarnings("FieldCanBeLocal")
public class PlayerActivity extends Activity implements SurfaceHolder.Callback, DemoPlayer.Listener,
    SidebarOverlayListener {
  public String API_KEY = "";
  private Context context = this;
  private VideoSurfaceView playerSurface;
  private ErrorPopup mErrorPopup;
  private JobManager mJobManager;
  private BufferingOverlay bufferingOverlay;
  private ControlOverlay mControlOverlay;
  private SidebarOverlay mSidebarOverlay;
  private boolean lastStateBuffering = false;
  public static final int TYPE_DASH = 0;
  public static final int TYPE_HLS = 2;
  private DemoPlayer player;
  private boolean playerNeedsPrepare;
  private Uri contentUri;
  private int contentType;
  private BitcodinJob.Source currentSource;
  private BitcodinJob currentJob;
  private long currentPlaybackPosition;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    Utils.hideSystemUI(this, true);

    setContentView(R.layout.activity_player);
    this.API_KEY = getIntent().getStringExtra("API_KEY");

    this.contentUri = Uri.parse(Settings.DEFAULT_PLAYER_URI);
    this.contentType = Settings.DEFAULT_PLAYER_TYPE;

    this.playerSurface = (VideoSurfaceView) findViewById(R.id.player_vsv_player_surface);
    this.playerSurface.getHolder().addCallback(this);

    Settings.init(this, this.API_KEY);
    Settings.WIDEVINE_GTS_DEFAULT_BASE_URI =
        this.context.getSharedPreferences("bitcodin", MODE_PRIVATE).
            getString(
                Settings.USER_FOLDER +
                    "widevine_url", "http://widevine-proxy.appspot.com/proxy");

    this.mJobManager = new JobManager(
        this,
        (LinearLayout) findViewById(R.id.player_llt_joblist_container),
        this.API_KEY
    );

    this.bufferingOverlay = new BufferingOverlay(
        (RelativeLayout) findViewById(R.id.player_rlt_buffering_overlay_wrapper),
        40
    );

    this.mControlOverlay = new ControlOverlay(
        this,
        (RelativeLayout) findViewById(R.id.player_rlt_overlay_wrapper),
        (LinearLayout) findViewById(R.id.player_llt_tooltip_wrapper),
        this.playerSurface
    );

    this.mSidebarOverlay = new SidebarOverlay(
        this,
        (LinearLayout) findViewById(R.id.player_llt_sidebar_wrapper),
        this.mJobManager
    );

    this.mControlOverlay.setTitle("Red Bull");
    this.mSidebarOverlay.addListener(this);

    this.mErrorPopup = new ErrorPopup(this);

    findViewById(R.id.player_llt_click_listener).setOnTouchListener(this.mOnTouchListener);
    findViewById(R.id.player_rlt_overlay_wrapper).setOnTouchListener(this.mOnTouchListener);

    this.currentPlaybackPosition = 0;

    releasePlayer();
    preparePlayer();
  }

  private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      showControls();
      mSidebarOverlay.show();
      Utils.hideSystemUI(context, true);
      return false;
    }
  };

  @Override
  public void onBackPressed() {
    this.finishAffinity();
  }

  @Override
  public void onResume() {
    super.onResume();
    preparePlayer(true, this.currentPlaybackPosition);
  }

  @Override
  public void onPause() {
    super.onPause();
    this.currentPlaybackPosition = this.player.getCurrentPosition();
    releasePlayer();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    releasePlayer();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    if (this.player != null) {
      this.player.setSurface(surfaceHolder.getSurface());
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    // do nothing
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    if (this.player != null) {
      this.player.blockingClearSurface();
    }
  }

  private DemoPlayer.RendererBuilder getRendererBuilder() {
    String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
    switch (this.contentType) {
      case TYPE_DASH:
        return new com.bitmovin.bitcodin.Player.DashRendererBuilder(
            this,
            userAgent,
            this.contentUri.toString(),
            new WidevineTestMediaDrmCallback()
        );
      case TYPE_HLS:
        return new com.bitmovin.bitcodin.Player.HlsRendererBuilder(
            this,
            userAgent,
            this.contentUri.toString()
        );
      default:
        throw new IllegalStateException("Unsupported type: " + this.contentType);
    }
  }

  private void preparePlayer() {
    preparePlayer(true, 0);
  }

  private void preparePlayer(boolean playWhenReady, long playbackPosition) {
    if (this.player == null) {
      this.player = new DemoPlayer(getRendererBuilder());
      this.player.addListener(this);
      this.playerNeedsPrepare = true;
      if (playbackPosition >= 0) {
        this.player.seekTo(playbackPosition);
      }
      this.mControlOverlay.setMediaPlayer(this.player.getPlayerControl());
    }
    if (this.playerNeedsPrepare) {
      this.player.prepare();
      this.playerNeedsPrepare = false;
    }
    this.player.setSurface(this.playerSurface.getHolder().getSurface());
    this.player.setPlayWhenReady(playWhenReady);
  }

  private void releasePlayer() {
    if (this.player != null) {
      this.player.release();
      this.player = null;
      System.gc();
    }
  }

  @Override
  public void onStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }

    switch (playbackState) {
      case ExoPlayer.STATE_BUFFERING:
        this.lastStateBuffering = true;
        this.bufferingOverlay.show();
        break;

      default:
        if (this.lastStateBuffering) {
          this.lastStateBuffering = false;
          this.bufferingOverlay.hide();
        }
        break;
    }
  }

  @Override
  public void onError(Exception e) {
    if (e instanceof ExoPlaybackException &&
        e.getCause() instanceof HttpDataSource.InvalidResponseCodeException &&
        ((HttpDataSource.InvalidResponseCodeException)e.getCause()).responseCode == 404
        && this.player.getCurrentPosition() > (this.player.getDuration() - 1000)) {
      releasePlayer();
      preparePlayer(false, 0);
    } else {
      this.mErrorPopup.show("Playback failed for job " + this.currentJob.getId() +
          "\n(" + this.currentSource.getSrcUrl() + ")");
      this.playerNeedsPrepare = true;
      showControls();
      this.mSidebarOverlay.show();
      e.printStackTrace();
    }
  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
    this.playerSurface.setVideoWidthHeightRatio(height == 0 ? 1 :
        (width * pixelWidthHeightRatio) / height);
  }

  private void showControls() {
    this.mControlOverlay.show();
  }

  @Override
  public void onSourceSelected(BitcodinJob.Source src, BitcodinJob job) {
    this.contentType = src.getType() == BitcodinJob.Source.Type.DASH ? TYPE_DASH : TYPE_HLS;
    this.contentUri = Uri.parse(src.getSrcUrl());
    this.currentJob = job;
    this.currentSource = src;
    releasePlayer();
    preparePlayer();
    this.mControlOverlay.setTitle("" + job.getId() + ": " + job.getInputFilename());
    this.mSidebarOverlay.hide();
    showControls();
  }
}
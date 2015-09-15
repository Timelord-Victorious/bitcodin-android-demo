package com.bitmovin.bitcodin.Overlay;

import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

public class BufferingOverlay extends Thread {
  private RelativeLayout bufferingOverlay;
  private RelativeLayout[] bufferingDots;
  private AlphaHandler mAlphaHandler;
  private Messenger mMessenger;
  private float[] startingOpacities = {0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f, 0.875f, 1};
  private long stepSizeMs;

  public BufferingOverlay(RelativeLayout bufferingOverlay, long stepSizeMs) {
    this.bufferingOverlay = bufferingOverlay;
    this.stepSizeMs = stepSizeMs;
    this.bufferingDots = new RelativeLayout[8];
    for (int idx = 0; idx < 8; idx++) {
      this.bufferingDots[idx] = (RelativeLayout) this.bufferingOverlay.getChildAt(idx);
    }
    this.start();
  }

  @Override
  public void run() {
    Looper.prepare();
    this.mAlphaHandler = new AlphaHandler(
        this.bufferingDots,
        this.startingOpacities,
        this.stepSizeMs
    );
    this.mMessenger = new Messenger(this.mAlphaHandler);
    Looper.loop();
  }

  public void show() {
    this.bufferingOverlay.post(new Runnable() {
      @Override
      public void run() {
        bufferingOverlay.setVisibility(View.VISIBLE);
      }
    });
    try {
      this.mMessenger.send(Message.obtain(this.mAlphaHandler, AlphaHandler.Action.UPDATE_OPACITY));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void hide() {
    this.bufferingOverlay.post(new Runnable() {
      @Override
      public void run() {
        bufferingOverlay.setVisibility(View.GONE);
      }
    });
    try {
      this.mMessenger.send(Message.obtain(this.mAlphaHandler, AlphaHandler.Action.STOP));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static class AlphaHandler extends android.os.Handler {
    private WeakReference<RelativeLayout[]> bufferingDotsRef;
    private float[] opacities;
    private float stepSizeMs;

    public AlphaHandler(RelativeLayout[] bufferingDots, float[] startOpacities, float stepSizeMs) {
      this.bufferingDotsRef = new WeakReference<>(bufferingDots);
      this.opacities = startOpacities;
      this.stepSizeMs = stepSizeMs;
    }

    private void setOpacities() {
      final RelativeLayout[] bufferingDots = this.bufferingDotsRef.get();
      for (int idx = 0; idx < 8; idx++) {
        opacities[idx] -= (stepSizeMs / 1000.0f);
        if (opacities[idx] < 0) {
          opacities[idx] = 1 - opacities[idx];
        }
        final int currentIdx = idx;
        bufferingDots[idx].post(new Runnable() {
          @Override
          public void run() {
            bufferingDots[currentIdx].setAlpha(opacities[currentIdx]);
          }
        });
      }
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case Action.UPDATE_OPACITY:
          setOpacities();
          if (this.hasMessages(Action.UPDATE_OPACITY)) break;
          this.sendEmptyMessageDelayed(Action.UPDATE_OPACITY, (long) this.stepSizeMs);
          break;

        case Action.STOP:
          this.removeMessages(Action.UPDATE_OPACITY);
          break;
      }
    }

    private final class Action {
      public static final int UPDATE_OPACITY = 1;
      public static final int STOP = 2;
    }
  }
}
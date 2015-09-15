package com.bitmovin.bitcodin.Overlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bitmovin.bitcodin.Job.BitcodinJob;
import com.bitmovin.bitcodin.Job.JobManager;
import com.bitmovin.bitcodin.Job.JobManagerListener;
import com.bitmovin.bitcodin.R;
import com.bitmovin.bitcodin.Settings;
import com.bitmovin.bitcodin.Utils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class SidebarOverlay implements JobManagerListener {
  private Context context;
  private JobManager mJobManager;
  private LinearLayout sidebarWrapper;

  /* sidebar header */
  private LinearLayout sidebarHeader;
  private ImageButton settingsButton;
  private EditText currentPage;
  private TextView totalPages;
  private ImageButton nextPage;
  private ImageButton previousPage;
  private ImageButton logout;

  /* loading view */
  private LinearLayout loadWrapper;
  private ProgressBar loadingProgressBar;

  /* joblist view */
  private LinearLayout joblistWrapper;
  private LinearLayout joblistContainer;
  private TextView noJobs;

  /* settings view */
  private LinearLayout settingsWrapper;
  private EditText defaultWideVineUri;

  /* sidebar handle */
  private RelativeLayout showSidebarWrapper;
  private ImageView showSidebarButton;

  /* animations */
  private SlideAnimation slideInSidebar;
  private SlideAnimation slideOutSidebar;
  private AlphaAnimation fadeInShowSidebarWrapper;
  private AlphaAnimation fadeOutShowSidebarWrapper;

  /* general */
  private boolean sidebarVisible;
  private boolean showSidebarWrapperVisible;
  private Timer fadeOutShowSidebarWrapperTimer;
  private TimerTask fadeOutShowSidebarWrapperTimerTask;
  private long numPages;
  private int lastPage;
  private ArrayList<SidebarOverlayListener> mListeners;

  private boolean loadingVisible;
  private boolean joblistVisible;
  private boolean settingsVisible;

  private SharedPreferences mPreferences;
  private SharedPreferences.Editor mPreferencesEditor;

  private enum FadingViews {
    SHOW_SIDEBAR_WRAPPER
  }

  public SidebarOverlay(final Context context, LinearLayout sidebarWrapper,
                        JobManager mJobManager) {
    this.context = context;
    this.sidebarWrapper = sidebarWrapper;
    this.mJobManager = mJobManager;

    if (this.sidebarWrapper != null) {

      /* sidebar header */
      this.sidebarHeader = (LinearLayout)
          this.sidebarWrapper.findViewById(R.id.player_llt_sidebar_header_container);
      this.settingsButton = (ImageButton)
          this.sidebarWrapper.findViewById(R.id.player_ib_settings);
      this.currentPage = (EditText)
          this.sidebarWrapper.findViewById(R.id.player_et_pager_current);
      this.totalPages = (TextView)
          this.sidebarWrapper.findViewById(R.id.player_tv_pager_total);
      this.nextPage = (ImageButton)
          this.sidebarWrapper.findViewById(R.id.player_ib_pager_next);
      this.previousPage = (ImageButton)
          this.sidebarWrapper.findViewById(R.id.player_ib_pager_prev);
      this.logout = (ImageButton)
          this.sidebarWrapper.findViewById(R.id.player_ib_logout);

      /* loading view */
      this.loadWrapper = (LinearLayout)
          this.sidebarWrapper.findViewById(R.id.player_llt_sidebar_loading);
      this.loadingProgressBar = (ProgressBar)
          this.sidebarWrapper.findViewById(R.id.player_pb_sidebar_loading_progress);

      /* joblist view */
      this.joblistWrapper = (LinearLayout)
          this.sidebarWrapper.findViewById(R.id.player_llt_sidebar_joblist);
      this.joblistContainer = (LinearLayout)
          this.sidebarWrapper.findViewById(R.id.player_llt_joblist_container);
      this.noJobs = (TextView)
          this.sidebarWrapper.findViewById(R.id.player_tv_no_jobs);

      /* settings view */
      this.settingsWrapper = (LinearLayout)
          this.sidebarWrapper.findViewById(R.id.player_llt_sidebar_settings);
      this.defaultWideVineUri = (EditText)
          this.settingsWrapper.findViewById(R.id.player_et_widevine_uri);

      /* sidebar handle */
      this.showSidebarWrapper = (RelativeLayout)
          this.sidebarWrapper.findViewById(R.id.player_rlt_show_sidebar);
      this.showSidebarButton = (ImageView)
          this.sidebarWrapper.findViewById(R.id.player_iv_show_sidebar);

      this.showSidebarWrapper.setOnClickListener(this.onShowSidebarWrapperClickListener);

      this.sidebarVisible = true;
      this.showSidebarWrapperVisible = true;

      this.numPages = -1;
      this.lastPage = -1;
      this.mListeners = new ArrayList<>();

      this.loadingVisible = false;
      this.joblistVisible = false;
      this.settingsVisible = false;

      this.settingsButton.setOnClickListener(this.onSettingsButtonClick);
      this.logout.setOnClickListener(this.onLogoutClickListener);

      this.defaultWideVineUri.setText(Settings.WIDEVINE_GTS_DEFAULT_BASE_URI);
      this.defaultWideVineUri.addTextChangedListener(this.onWideVineUriChanged);

      this.mPreferences = this.context.getSharedPreferences("bitcodin", Context.MODE_PRIVATE);
      this.mPreferencesEditor = this.mPreferences.edit();

      setupAnimations();

      this.mJobManager.addListener(this);
      this.mJobManager.loadJobs(0);
    }
  }

  public void addListener(SidebarOverlayListener listener) {
    if (listener != null) {
      this.mListeners.add(listener);
    }
  }

  // region animations and visibility

  private void setupAnimations() {

    this.slideInSidebar = new SlideAnimation(
        this.sidebarWrapper,
        Settings.SLIDE_DURATION,
        SlideAnimation.Direction.LEFT,
        -this.context.getResources().getDimension(R.dimen.player_rlt_sidebar_width),
        0);
    this.slideInSidebar.setAnimationListener(this.sidebarAnimationListener);

    this.slideOutSidebar = new SlideAnimation(
        this.sidebarWrapper,
        Settings.SLIDE_DURATION,
        SlideAnimation.Direction.LEFT,
        0,
        -this.context.getResources().getDimension(R.dimen.player_rlt_sidebar_width));
    this.slideOutSidebar.setAnimationListener(this.sidebarAnimationListener);

    this.fadeInShowSidebarWrapper = new AlphaAnimation(0, 1);
    this.fadeInShowSidebarWrapper.setDuration(Settings.FADE_IN_DURATION);
    this.fadeInShowSidebarWrapper.setAnimationListener(this.sidebarAnimationListener);

    this.fadeOutShowSidebarWrapper = new AlphaAnimation(1, 0);
    this.fadeOutShowSidebarWrapper.setDuration(Settings.FADE_OUT_DURATION);
    this.fadeOutShowSidebarWrapper.setAnimationListener(this.sidebarAnimationListener);
  }

  private Animation.AnimationListener sidebarAnimationListener = new Animation.AnimationListener() {
    @Override
    public void onAnimationStart(Animation animation) {
      if (animation == fadeInShowSidebarWrapper) {
        showSidebarWrapper.setVisibility(View.VISIBLE);
        showSidebarWrapperVisible = true;
      } else if (animation == fadeOutShowSidebarWrapper) {
        showSidebarWrapperVisible = false;
      }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
      if (animation == slideInSidebar) {
        showSidebarButton.setBackgroundResource(R.drawable.collapse_sidebar);
        sidebarVisible = true;
      } else if (animation == slideOutSidebar) {
        showSidebarButton.setBackgroundResource(R.drawable.expand_sidebar);
        sidebarVisible = false;
        fadeView(false, FadingViews.SHOW_SIDEBAR_WRAPPER, false);
      } else if (animation == fadeOutShowSidebarWrapper) {
        showSidebarWrapper.setVisibility(View.GONE);
      } else if (animation == fadeInShowSidebarWrapper) {
        setupFadeoutTimer();
      }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
  };

  private void setupFadeoutTimer() {
    this.fadeOutShowSidebarWrapperTimerTask = new TimerTask() {
      @Override
      public void run() {
        showSidebarWrapper.post(new Runnable() {
          @Override
          public void run() {
            fadeView(false, FadingViews.SHOW_SIDEBAR_WRAPPER, false);
          }
        });
      }
    };
    this.fadeOutShowSidebarWrapperTimer = new Timer();
    this.fadeOutShowSidebarWrapperTimer.schedule(
        fadeOutShowSidebarWrapperTimerTask,
        Settings.FADE_OUT_TIMEOUT
    );
  }

  public void show() {
    if (!this.sidebarVisible) {
      fadeView(true, FadingViews.SHOW_SIDEBAR_WRAPPER, false);
    }
  }

  public void hide() {
    if (this.sidebarVisible) {
      this.sidebarWrapper.startAnimation(this.slideOutSidebar);
      this.sidebarVisible = false;
    }
    fadeView(false, FadingViews.SHOW_SIDEBAR_WRAPPER, true);
  }

  private void fadeView(boolean fadeIn, FadingViews view, boolean force) {
    Animation mAnimation;
    switch (view) {
      case SHOW_SIDEBAR_WRAPPER:
        if (!(this.showSidebarWrapperVisible ^ fadeIn) && !force) break;
        if (!fadeIn && this.sidebarVisible && !force) break;
        mAnimation = fadeIn ? this.fadeInShowSidebarWrapper : this.fadeOutShowSidebarWrapper;
        this.showSidebarWrapper.clearAnimation();
        mAnimation.reset();
        this.showSidebarWrapper.setAnimation(mAnimation);
        mAnimation.start();
        if (!fadeIn) {
          try {
            this.fadeOutShowSidebarWrapperTimer.cancel();
          } catch (Exception ignore) {
          }
        }
        break;
    }
  }

  private void setNoJobsVisible(final boolean visible) {
    this.noJobs.post(new Runnable() {
      @Override
      public void run() {
        noJobs.setVisibility(visible ? View.VISIBLE : View.GONE);
      }
    });
  }

  private void setSidebarHeaderEnabled(final boolean enabled) {
    this.sidebarHeader.post(new Runnable() {
      @Override
      public void run() {
        int numChildren = sidebarHeader.getChildCount();
        for (int idx = 0; idx < numChildren; idx++) {
          sidebarHeader.getChildAt(idx).setEnabled(enabled);
        }
      }
    });
  }

  private void setLoadingViewVisible(final boolean visible, boolean updateState) {
    if (updateState) this.loadingVisible = visible;
    if (this.settingsVisible && updateState) return;
    this.loadWrapper.post(new Runnable() {
      @Override
      public void run() {
        loadWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
      }
    });
  }

  private void setJoblistViewVisible(final boolean visible, boolean updateState) {
    if (updateState) this.joblistVisible = visible;
    if (this.settingsVisible && updateState) return;
    this.joblistWrapper.post(new Runnable() {
      @Override
      public void run() {
        joblistWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
      }
    });
  }

  private void setSettingsVisible(final boolean visible) {
    this.settingsVisible = visible;
    if (visible) {
      setLoadingViewVisible(false, false);
      setJoblistViewVisible(false, false);
    }
    this.settingsWrapper.post(new Runnable() {
      @Override
      public void run() {
        settingsWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
      }
    });
    if (!visible) {
      setLoadingViewVisible(this.loadingVisible, false);
      setJoblistViewVisible(this.joblistVisible, false);
    }
  }

  // endregion animations and visibility

  // region jobManager callbacks

  @Override
  public void onStartLoading() {
    setLoadingViewVisible(true, true);
    setJoblistViewVisible(false, true);
    setSidebarHeaderEnabled(false);
  }

  @Override
  public void onNumJobsAvailable(final long numJobs, long perPage) {

    this.numPages = (int) Math.ceil((float) numJobs / (float) perPage);
    this.previousPage.setOnClickListener(this.onPagePrevNextClickListener);
    this.nextPage.setOnClickListener(this.onPagePrevNextClickListener);
    this.currentPage.setOnKeyListener(this.currentPageKeyListener);

    this.sidebarWrapper.post(new Runnable() {
      @Override
      public void run() {
        totalPages.setText("/ " + ((numPages > 0) ? numPages : "1"));
        if (lastPage > 0) {
          currentPage.setText("" + lastPage);
        } else {
          currentPage.setText("1");
          lastPage = 1;
        }
      }
    });
  }

  @Override
  public void onProgressChanged(final double progress) {
    //this.loadingProgressBar.post(new Runnable() {
    //    @Override
    //    public void run() {
    //        loadingProgressBar.setProgress((int) (progress * 1000.0));
    //    }
    //});
  }

  @Override
  public void onJobsLoaded(int numJobs) {
    setLoadingViewVisible(false, true);
    setJoblistViewVisible(true, true);
    setSidebarHeaderEnabled(true);
    if (numJobs <= 0) {
      setNoJobsVisible(true);
    } else {
      setNoJobsVisible(false);
    }
  }

  @Override
  public void onSourceSelected(BitcodinJob.Source src, BitcodinJob job) {
    if (this.mListeners != null) {
      for (SidebarOverlayListener listener : this.mListeners) {
        listener.onSourceSelected(src, job);
      }
    }
  }

  // endregion jobManager callbacks

  // region event listeners

  private View.OnClickListener onLogoutClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      SharedPreferences.Editor mSharedPreferenceEditor =
          context.getSharedPreferences("bitcodin", Context.MODE_PRIVATE).edit();
      mSharedPreferenceEditor.remove("current_api_key");
      mSharedPreferenceEditor.commit();
      ((Activity)context).finishAffinity();
    }
  };

  private View.OnClickListener onSettingsButtonClick = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      setSettingsVisible(settingsVisible = !settingsVisible);
    }
  };

  private TextWatcher onWideVineUriChanged = new TextWatcher() {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
      Settings.WIDEVINE_GTS_DEFAULT_BASE_URI = defaultWideVineUri.getText().toString();
      mPreferencesEditor.putString(
          Settings.USER_FOLDER + "widevine_url",
          Settings.WIDEVINE_GTS_DEFAULT_BASE_URI
      );
      mPreferencesEditor.commit();
    }
  };

  private View.OnKeyListener currentPageKeyListener = new View.OnKeyListener() {
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        int currentVal = parseIntSafe(currentPage.getText().toString(), -1);
        if (currentVal < 1) {
          currentPage.setText("1");
          if (lastPage != 1) {
            lastPage = 1;
            mJobManager.loadJobs(lastPage);
          }
        } else if (currentVal > numPages) {
          currentPage.setText("" + numPages);
          if (lastPage != (int) numPages) {
            lastPage = (int) numPages;
            mJobManager.loadJobs(lastPage);
          }
        } else {
          lastPage = currentVal;
          mJobManager.loadJobs(lastPage);
        }
        InputMethodManager imm = (InputMethodManager)
            context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        Utils.hideSystemUI(context, true);
        return true;
      } else if (keyCode == KeyEvent.KEYCODE_BACK) {
        Utils.hideSystemUI(context, true);
        return false;
      }
      return false;
    }
  };

  private View.OnClickListener onPagePrevNextClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      int currentValue = parseIntSafe(currentPage.getText().toString(), -1);
      if (currentValue > numPages) currentValue = (int) numPages;
      if (currentValue < 1) currentValue = 1;
      currentPage.setText("" + currentValue);

      if (v == previousPage) {
        if (currentValue > 1) {
          lastPage = currentValue - 1;
          currentPage.setText("" + lastPage);
        }
      } else if (v == nextPage) {
        if (currentValue < numPages) {
          lastPage = currentValue + 1;
          currentPage.setText("" + lastPage);
        }
      }
      mJobManager.loadJobs(lastPage);
    }
  };

  private View.OnClickListener onShowSidebarWrapperClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (!sidebarVisible) {
        fadeView(true, FadingViews.SHOW_SIDEBAR_WRAPPER, false);
        try {
          fadeOutShowSidebarWrapperTimer.cancel();
        } catch (Exception ignore) {
        }
        sidebarWrapper.startAnimation(slideInSidebar);
        sidebarVisible = true;
      } else {
        sidebarWrapper.startAnimation(slideOutSidebar);
        sidebarVisible = false;
      }
    }
  };

  // endregion event listeners

  private int parseIntSafe(String str, int defValue) {
    try {
      return Integer.parseInt(str);
    } catch (Exception ignore) {
      return defValue;
    }
  }

  private class SlideAnimation extends Animation {
    private View mView;
    private RelativeLayout.LayoutParams mLayoutParams;
    private String direction;
    private float pxStart;
    private float pxEnd;

    public SlideAnimation(View mView, long duration, String direction, float pxStart, float pxEnd) {
      this.mView = mView;
      this.mLayoutParams = (RelativeLayout.LayoutParams) this.mView.getLayoutParams();
      this.direction = direction;
      this.pxStart = pxStart;
      this.pxEnd = pxEnd;
      this.setDuration(duration);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
      int currentPx = (int) (pxStart + ((pxEnd - pxStart) * interpolatedTime));
      switch (this.direction) {
        case Direction.LEFT:
          this.mLayoutParams.leftMargin = currentPx;
          break;

        case Direction.RIGHT:
          this.mLayoutParams.rightMargin = currentPx;
          break;

        case Direction.TOP:
          this.mLayoutParams.topMargin = currentPx;
          break;

        case Direction.BOTTOM:
          this.mLayoutParams.bottomMargin = currentPx;
          break;
      }
      this.mView.setLayoutParams(this.mLayoutParams);
    }


    public final class Direction {
      public static final String LEFT = "left";
      public static final String RIGHT = "right";
      public static final String TOP = "top";
      public static final String BOTTOM = "bottom";
    }
  }
}
package com.bitmovin.bitcodin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import java.io.File;

public class Utils {
  public static boolean makeDirs(String path) {
    File dir = new File(path);
    return !(dir.exists() && dir.isDirectory()) && dir.mkdirs();
  }

  public static float toPx(Context context, float dp) {
    if (context == null) return 0;
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
        context.getResources().getDisplayMetrics());
  }

  @SuppressLint("InlinedApi")
  public static void hideSystemUI(Context context, boolean keepScreenOn) {

    ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    );

    if (keepScreenOn) ((Activity) context).getWindow().addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    );

    ((Activity) context).getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            ((Build.VERSION.SDK_INT >= 19) ?
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY :
                View.SYSTEM_UI_FLAG_LOW_PROFILE)
    );

    ((Activity) context).getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
    );
  }
}
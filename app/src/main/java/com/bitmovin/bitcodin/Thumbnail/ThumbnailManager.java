package com.bitmovin.bitcodin.Thumbnail;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.bitmovin.bitcodin.R;

import java.util.HashMap;


@SuppressWarnings("FieldCanBeLocal")
public class ThumbnailManager {
  private Context context;
  private HashMap<String, Drawable> mThumbnails;
  private Drawable blankDrawable;

  /* getDrawable(id) is deprecated, but getDrawable(id, theme) requires API 21 */
  @SuppressWarnings("deprecation")
  public ThumbnailManager(Context context) {
    this.context = context;
    this.mThumbnails = new HashMap<>();
    this.blankDrawable = this.context.getResources().getDrawable(R.drawable.no_thumbnail);
  }

  public void add(String path) {
    if (!this.mThumbnails.containsKey(path)) {
      this.mThumbnails.put(path, null);
    }
  }

  public Drawable get(String path) {
    if (this.mThumbnails != null && this.mThumbnails.containsKey(path)) {
      if (this.mThumbnails.get(path) == null) {
        this.mThumbnails.put(path, Drawable.createFromPath(path));
      }
      return this.mThumbnails.get(path);
    } else {
      return this.blankDrawable;
    }
  }
}
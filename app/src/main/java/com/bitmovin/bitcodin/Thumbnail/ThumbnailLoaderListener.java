package com.bitmovin.bitcodin.Thumbnail;

public interface ThumbnailLoaderListener {
  void onThumbnailLoaded(String thumbnailPath);

  void onError(Throwable th);
}
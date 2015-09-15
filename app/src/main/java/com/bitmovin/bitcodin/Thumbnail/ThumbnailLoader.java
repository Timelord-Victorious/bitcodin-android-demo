package com.bitmovin.bitcodin.Thumbnail;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.bitmovin.bitcodin.Settings;
import com.bitmovin.bitcodin.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

@SuppressWarnings("FieldCanBeLocal")
public class ThumbnailLoader extends Thread {
  private static final String TAG = "ThumbnailLoader";
  private MessageHandler mMessageHandler;
  private Messenger mMessenger;
  private File thumbnailFile;
  private InputStream mInputStream;
  private OutputStream mOutputStream;
  private byte[] buffer;
  private ThumbnailManager mThumbnailManager;

  public ThumbnailLoader(ThumbnailManager mThumbnailManager) {
    this.mThumbnailManager = mThumbnailManager;
    this.buffer = new byte[1024];
    this.start();
  }

  @Override
  public void run() {
    Looper.prepare();
    mMessageHandler = new MessageHandler(this);
    mMessenger = new Messenger(mMessageHandler);
    Log.i(TAG, "starting looper...");
    Looper.loop();
  }

  public String load(String url) {
    downloadThumbnail(new ThumbnailJob(url, null));
    return this.thumbnailFile.getAbsolutePath();
  }

  public void loadAsync(String url, ThumbnailLoaderListener listener) {
    try {
      this.mMessenger.send(Message.obtain(
          null,
          MessageHandler.Action.DOWNLOAD,
          new ThumbnailJob(
              url,
              listener
          )));
    } catch (Exception ex) {
      listener.onError(ex);
    }
  }

  private void downloadThumbnail(ThumbnailJob data) {
    try {
      Utils.makeDirs(Settings.THUMBNAIL_CACHE);
      this.thumbnailFile = new File(
          Settings.THUMBNAIL_CACHE + data.url.substring(data.url.lastIndexOf("/"))
      );
      if (!this.thumbnailFile.exists()) {
        this.mInputStream = new java.net.URL(data.url).openStream();
        this.mOutputStream = new FileOutputStream(thumbnailFile, false);
        int len;
        while ((len = this.mInputStream.read(buffer)) > 0) {
          this.mOutputStream.write(buffer, 0, len);
        }
        this.mInputStream.close();
        this.mOutputStream.flush();
        this.mOutputStream.close();
      }
      this.mThumbnailManager.add(this.thumbnailFile.getAbsolutePath());
      if (data.listener != null) {
        data.listener.onThumbnailLoaded(thumbnailFile.getAbsolutePath());
      }
    } catch (Exception ex) {
      if (data.listener != null) {
        data.listener.onError(ex);
      }
    }
  }

  private static class MessageHandler extends Handler {
    private WeakReference<ThumbnailLoader> mThumbnailLoaderRef;

    public MessageHandler(ThumbnailLoader mThumbnailLoader) {
      this.mThumbnailLoaderRef = new WeakReference<>(mThumbnailLoader);
    }

    public static class Action {
      public static final int DOWNLOAD = 0;
    }

    @Override
    public void handleMessage(Message msg) {
      ThumbnailLoader mThumbnailLoader = this.mThumbnailLoaderRef.get();
      switch (msg.what) {
        case Action.DOWNLOAD:
          mThumbnailLoader.downloadThumbnail((ThumbnailJob) msg.obj);
          break;
      }
    }
  }

  private static class ThumbnailJob {
    String url;
    ThumbnailLoaderListener listener;

    public ThumbnailJob(String url, ThumbnailLoaderListener listener) {
      this.url = url;
      this.listener = listener;
    }
  }
}
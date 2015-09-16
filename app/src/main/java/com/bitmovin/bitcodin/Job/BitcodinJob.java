package com.bitmovin.bitcodin.Job;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bitmovin.bitcodin.Thumbnail.ThumbnailLoader;
import com.bitmovin.bitcodin.Thumbnail.ThumbnailLoaderListener;
import com.bitmovin.bitcodin.api.job.JobDetails;

import java.util.ArrayList;


public class BitcodinJob implements ThumbnailLoaderListener {
  private int id;
  private String inputFilename;
  private ArrayList<Source> sources;
  private String thumbnailUrl;
  private String thumbnail;
  private boolean thumbnailLoaded = false;
  private ImageView thumbnailHolder;
  private ThumbnailLoader mThumbnailLoader;
  private boolean thumbnailLoading = false;

  public BitcodinJob(JobDetails mJobDetails, ThumbnailLoader mThumbnailLoader) {
    this(
        mJobDetails.jobId,
        mJobDetails.input.filename,
        mJobDetails.input.thumbnailUrl,
        mThumbnailLoader
    );

    if (mJobDetails.manifestUrls.mpdUrl != null && !mJobDetails.manifestUrls.mpdUrl.equals("")) {
      addSource(mJobDetails.manifestUrls.mpdUrl, BitcodinJob.Source.Type.DASH);
    }

    if (mJobDetails.manifestUrls.m3u8Url != null && !mJobDetails.manifestUrls.m3u8Url.equals("")) {
      addSource(mJobDetails.manifestUrls.m3u8Url, BitcodinJob.Source.Type.HLS);
    }

    loadThumbnail();
  }

  public BitcodinJob(int id, String inputFilename, String thumbnailUrl,
                     ThumbnailLoader mThumbnailLoader) {
    this.id = id;
    this.inputFilename = inputFilename;
    this.sources = new ArrayList<>();
    this.thumbnailUrl = thumbnailUrl;
    this.thumbnail = null;
    this.thumbnailHolder = null;
    this.mThumbnailLoader = mThumbnailLoader;
  }

  public int getId() {
    return this.id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getInputFilename() {
    return this.inputFilename;
  }

  public void setInputFilename(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  public String getThumbnailUrl() {
    return this.thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    setThumbnailUrl(thumbnailUrl, false);
  }

  public void setThumbnailUrl(String thumbnailUrl, boolean loadThumbnail) {
    this.thumbnailUrl = thumbnailUrl;
    if (loadThumbnail) loadThumbnailAsync();
  }

  public String getThumbnail() {
    return this.thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public void setThumbnailHolder(ImageView thumbnailHolder, boolean setOnLoad) {
    if (thumbnailHolder == null) return;

    this.thumbnailHolder = thumbnailHolder;
    if (setOnLoad) {
      if (this.thumbnailLoaded) setThumbnailHolderBackground();
      else loadThumbnailAsync();
    }
  }

  public int getNumSources() {
    return this.sources.size();
  }

  public ArrayList<Source> getSources() {
    return this.sources;
  }

  public void setSources(ArrayList<Source> sources) {
    this.sources = sources;
  }

  public void addSource(Source src) {
    if (this.sources != null) {
      this.sources.add(src);
    }
  }

  public void addSource(String srcUrl, int type) {
    addSource(new Source(srcUrl, type));
  }

  public Source getSource() {
    return getSourceAt(0);
  }

  public boolean hasSource(int type) {
    if (this.sources == null || this.sources.size() == 0) {
      return false;
    } else {
      for (Source src : this.sources) {
        if (src.getType() == type) {
          return true;
        }
      }
      return false;
    }
  }

  public Source getSource(int type) {
    for (Source src : this.sources) {
      if (src.getType() == type) {
        return src;
      }
    }
    return null;
  }

  public Source getSourceAt(int index) {
    if (this.sources == null) return null;
    if (index >= this.sources.size()) return null;
    else return this.sources.get(index);
  }

  public boolean removeSource() {
    return removeSource(0);
  }

  public boolean removeSource(int index) {
    if (this.sources == null) return false;
    if (index >= this.sources.size()) return false;
    this.sources.remove(index);
    return true;
  }

  public boolean removeSource(Source src) {
    return this.sources != null && this.sources.remove(src);
  }

  public void loadThumbnail() {
    this.thumbnailLoaded = true;
    this.thumbnailLoading = false;
    this.thumbnail = this.mThumbnailLoader.load((
        (this.thumbnailUrl.startsWith("//") ? "http:" : "") +
            this.thumbnailUrl).replace(" ", "%20"));
    setThumbnailHolderBackground();
  }

  private void loadThumbnailAsync() {
    if (!this.thumbnailLoading) {
      this.thumbnailLoading = true;
      this.mThumbnailLoader.loadAsync((
          (this.thumbnailUrl.startsWith("//") ? "http:" : "") +
              this.thumbnailUrl).replace(" ", "%20"), this);
    }
  }

  private void setThumbnailHolderBackground() {
    if (this.thumbnailHolder != null) {
      this.thumbnailHolder.post(new Runnable() {
        @Override
        public void run() {
          thumbnailHolder.setImageDrawable(Drawable.createFromPath(thumbnail));
        }
      });
    }
  }

  @Override
  public void onThumbnailLoaded(String thumbnailPath) {
    this.thumbnailLoaded = true;
    this.thumbnailLoading = false;
    this.thumbnail = thumbnailPath;
    setThumbnailHolderBackground();
  }

  @Override
  public void onError(Throwable th) {
    th.printStackTrace();
  }

  public class Source {
    private String srcUrl;
    private int type;

    public Source(String srcUrl, int type) {
      this.srcUrl = srcUrl;
      this.type = type;
    }

    public String getSrcUrl() {
      return this.srcUrl;
    }

    public void setSrcUrl(String srcUrl) {
      this.srcUrl = srcUrl;
    }

    public int getType() {
      return this.type;
    }

    public String getTypeStr() {
      switch (this.type) {
        case Type.DASH:
          return "DASH";
        case Type.HLS:
          return "HLS";
        default:
          return "UNKNOWN";
      }
    }

    public void setType(int type) {
      this.type = type;
    }

    public final class Type {
      public static final int DASH = 0;
      public static final int HLS = 1;
      /* TODO: DRM etc. */
      public static final int OTHER = -1;
    }
  }
}
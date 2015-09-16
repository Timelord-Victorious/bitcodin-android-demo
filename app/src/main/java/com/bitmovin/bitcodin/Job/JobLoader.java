package com.bitmovin.bitcodin.Job;

import com.bitmovin.bitcodin.Settings;
import com.bitmovin.bitcodin.Thumbnail.ThumbnailLoader;
import com.bitmovin.bitcodin.Thumbnail.ThumbnailManager;
import com.bitmovin.bitcodin.api.BitcodinApi;
import com.bitmovin.bitcodin.api.job.JobDetails;
import com.bitmovin.bitcodin.api.job.JobList;
import com.bitmovin.bitcodin.api.job.JobStatus;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class JobLoader {
  private BitcodinApi mBitcodinApi;
  private JobLoaderListener mJobLoaderCallback;
  private ArrayList<BitcodinJob> mBitcodinJobs;
  private ThumbnailLoader mThumbnailLoader;
  private long numJobs;
  private int page;
  private boolean isLoading;
  private Thread loaderThread;
  private Runnable loaderRunnable;

  public JobLoader(BitcodinApi mBitcodinApi, JobLoaderListener jobLoadedCallback,
                   ThumbnailManager mThumbnailManager) {
    this.mBitcodinApi = mBitcodinApi;
    this.mJobLoaderCallback = jobLoadedCallback;
    this.mThumbnailLoader = new ThumbnailLoader(mThumbnailManager);
    this.mBitcodinJobs = new ArrayList<>();
    this.numJobs = -1;
    this.isLoading = false;
  }

  public boolean loadPage(int page) {
    if (!isLoading) {
      this.mBitcodinJobs.clear();
      this.isLoading = true;
      this.page = page;
      this.numJobs = -1;
      this.loaderRunnable = new Runnable() {
        @Override
        public void run() {
          loadJobs();
        }
      };
      this.loaderThread = new Thread(this.loaderRunnable);
      this.loaderThread.start();
      return true;
    } else {
      return false;
    }
  }

  private void loadJobs() {
    try {
      ApiLoader mLoader = new ApiLoader(this.mBitcodinApi);
      mLoader.setPage(this.page);
      int jobIdx = 0;

      if (this.numJobs < 0) {
        this.mJobLoaderCallback.onNumJobsAvailable(mLoader.getNumJobs(), mLoader.getNumJobsPerPage());
        this.numJobs = mLoader.getNumJobs();
      }

      while (mLoader.hasNext()) {
        JobDetails mJob = mLoader.getNext();
        if (mJob.status == JobStatus.FINISHED) {
          if (!Settings.DASH_ONLY ||
              (mJob.manifestUrls.mpdUrl != null && !mJob.manifestUrls.mpdUrl.equals(""))) {
            this.mBitcodinJobs.add(new BitcodinJob(mJob, this.mThumbnailLoader));
          }
        }
        jobIdx++;
        this.mJobLoaderCallback.onProgressChanged((double) jobIdx / (double) mLoader.getNumJobsPerPage());
      }
      this.mBitcodinJobs.trimToSize();
      this.mJobLoaderCallback.onJobLoaded(this.mBitcodinJobs);
      this.isLoading = false;
    } catch (Exception ex) {
      ex.printStackTrace();
      this.mJobLoaderCallback.onJobLoaded(null);
    }
  }

  private class ApiLoader {
    private BitcodinApi mBitcodinApi;
    private long numJobs;
    private long numJobsPerPage;
    private int page;
    private List<JobDetails> currentJobs;

    public ApiLoader(BitcodinApi mBitcodinApi) {
      this.mBitcodinApi = mBitcodinApi;
      this.currentJobs = new ArrayList<>();
    }

    public void setPage(int page) {
      this.currentJobs = new ArrayList<>();
      this.page = page;
      try {
        JobList mJobList = this.mBitcodinApi.listJobs(this.page);
        this.currentJobs = mJobList.jobs;
        this.numJobsPerPage = mJobList.perPage;
        this.numJobs = mJobList.totalCount;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    JobDetails getNext() {
      return this.currentJobs.remove(0);
    }

    boolean hasNext() {
      return this.currentJobs.size() > 0;
    }

    long getNumJobs() {
      return this.numJobs;
    }

    long getNumJobsPerPage() {
      return this.numJobsPerPage;
    }
  }
}
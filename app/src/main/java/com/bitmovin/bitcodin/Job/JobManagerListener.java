package com.bitmovin.bitcodin.Job;

public interface JobManagerListener {
  void onStartLoading();

  void onNumJobsAvailable(long numJobs, long perPage);

  void onProgressChanged(double progress);

  void onJobsLoaded(int numJobs);

  void onSourceSelected(BitcodinJob.Source src, BitcodinJob job);
}
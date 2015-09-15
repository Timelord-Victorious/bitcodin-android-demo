package com.bitmovin.bitcodin.Job;

import java.util.ArrayList;

public interface JobLoaderListener {
  void onNumJobsAvailable(long numJobs, long perPage);

  void onJobLoaded(ArrayList<BitcodinJob> jobs);

  void onJobChanged(ArrayList<BitcodinJob> jobs);

  void onProgressChanged(double progress);
}
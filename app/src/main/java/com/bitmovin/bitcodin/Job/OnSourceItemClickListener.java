package com.bitmovin.bitcodin.Job;

import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class OnSourceItemClickListener implements View.OnClickListener {
  private WeakReference<BitcodinJob> mJobRef;
  private WeakReference<BitcodinJob.Source> mSourceRef;
  private WeakReference<ArrayList<JobManagerListener>> mListenersRef;

  public OnSourceItemClickListener(BitcodinJob job, BitcodinJob.Source src, ArrayList<JobManagerListener> listeners) {
    this.mJobRef = new WeakReference<>(job);
    this.mSourceRef = new WeakReference<>(src);
    this.mListenersRef = new WeakReference<>(listeners);
  }

  @Override
  public void onClick(final View v) {
    for (JobManagerListener listener : this.mListenersRef.get()) {
      listener.onSourceSelected(this.mSourceRef.get(), this.mJobRef.get());
    }
  }
}
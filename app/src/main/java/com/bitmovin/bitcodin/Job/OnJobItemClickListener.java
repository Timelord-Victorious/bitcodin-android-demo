package com.bitmovin.bitcodin.Job;

import android.view.View;

import com.bitmovin.bitcodin.R;
import com.bitmovin.bitcodin.Settings;

import java.util.ArrayList;

public class OnJobItemClickListener implements View.OnClickListener {
  private static View lastClickedJobItem;
  private BitcodinJob mJob;
  private ArrayList<JobManagerListener> listeners;

  public OnJobItemClickListener(BitcodinJob mJob, ArrayList<JobManagerListener> listeners) {
    lastClickedJobItem = null;
    this.mJob = mJob;
    this.listeners = listeners;
  }

  @Override
  public void onClick(final View v) {
    if (!Settings.DASH_ONLY) {
      if (lastClickedJobItem != null && lastClickedJobItem != v) {
        lastClickedJobItem.findViewById(R.id.sourceContainer).setVisibility(View.GONE);
      }
      (lastClickedJobItem = v).findViewById(R.id.sourceContainer).setVisibility(View.VISIBLE);
    } else {
      if (this.listeners != null && this.mJob.hasSource(BitcodinJob.Source.Type.DASH)) {
        for (JobManagerListener listener : this.listeners) {
          listener.onSourceSelected(this.mJob.getSource(BitcodinJob.Source.Type.DASH), this.mJob);
        }
      }
    }
  }
}
package com.bitmovin.bitcodin.Job;

import android.view.View;

import com.bitmovin.bitcodin.R;

public class OnJobItemClickListener implements View.OnClickListener {
  private static View lastClickedJobItem;

  public OnJobItemClickListener() {
    lastClickedJobItem = null;
  }

  @Override
  public void onClick(final View v) {
    if (lastClickedJobItem != null && lastClickedJobItem != v) {
      lastClickedJobItem.findViewById(R.id.sourceContainer).setVisibility(View.GONE);
    }
    (lastClickedJobItem = v).findViewById(R.id.sourceContainer).setVisibility(View.VISIBLE);
  }
}
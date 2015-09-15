package com.bitmovin.bitcodin.Overlay;

import com.bitmovin.bitcodin.Job.BitcodinJob;

public interface SidebarOverlayListener {
  void onSourceSelected(BitcodinJob.Source src, BitcodinJob job);
}
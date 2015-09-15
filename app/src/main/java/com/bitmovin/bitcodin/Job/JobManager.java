package com.bitmovin.bitcodin.Job;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bitmovin.bitcodin.R;
import com.bitmovin.bitcodin.Thumbnail.ThumbnailManager;
import com.bitmovin.bitcodin.api.BitcodinApi;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class JobManager implements JobLoaderListener {
  private LinearLayout jobContainerLayout;
  private BitcodinApi mBitcodinApi;
  private JobLoader mJobLoader;
  private ThumbnailManager mThumbnailManager;
  private Context context;
  private ArrayList<JobManagerListener> listeners;
  private ArrayList<BitcodinJob> mJobs;

  private LayoutInflater mLayoutInflater;

  public JobManager(Context context, LinearLayout jobContainerLayout, String apiKey) {
    this.jobContainerLayout = jobContainerLayout;
    this.context = context;

    this.mThumbnailManager = new ThumbnailManager(context);
    this.listeners = new ArrayList<>();

    this.mBitcodinApi = new BitcodinApi(apiKey);
    this.mJobLoader = new JobLoader(this.mBitcodinApi, this, this.mThumbnailManager);

    this.mLayoutInflater = (LayoutInflater)
        this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public void addListener(JobManagerListener listener) {
    if (this.listeners != null) {
      this.listeners.add(listener);
    }
  }

  public void loadJobs(int page) {
    this.mJobLoader.loadPage(page);
    if (this.listeners != null) {
      for (JobManagerListener listener : this.listeners) {
        listener.onStartLoading();
      }
    }
  }

  @Override
  public void onNumJobsAvailable(long numJobs, long perPage) {
    if (this.listeners != null) {
      for (JobManagerListener listener : this.listeners) {
        listener.onNumJobsAvailable(numJobs, perPage);
      }
    }
  }

  @Override
  public void onJobLoaded(ArrayList<BitcodinJob> jobs) {
    this.mJobs = jobs;

    if (this.jobContainerLayout.getChildCount() > 0) {
      this.jobContainerLayout.post(new Runnable() {
        @Override
        public void run() {
          jobContainerLayout.removeAllViews();
        }
      });
    }

    for (final BitcodinJob job : this.mJobs) {
      final View currentView = createJobView(job);
      this.jobContainerLayout.post(new Runnable() {
        @Override
        public void run() {
          jobContainerLayout.addView(currentView);
        }
      });
    }

    if (this.listeners != null) {
      for (JobManagerListener listener : this.listeners) {
        listener.onJobsLoaded(jobs.size());
      }
    }
  }

  @Override
  public void onJobChanged(ArrayList<BitcodinJob> jobs) {
    onJobLoaded(jobs);
  }

  private View createJobView(BitcodinJob job) {

    View jobWrapper = this.mLayoutInflater.inflate(
        R.layout.job_item,
        this.jobContainerLayout,
        false
    );

    LinearLayout sourceWrapper = (LinearLayout) jobWrapper.findViewById(R.id.sourceContainer);

    ImageView thumbnailIV = (ImageView) jobWrapper.findViewById(R.id.jobThumbnailIV);
    TextView jobIdTV = (TextView) jobWrapper.findViewById(R.id.jobIdTV);
    TextView jobInputfileTV = (TextView) jobWrapper.findViewById(R.id.jobInputfileTV);

    thumbnailIV.setImageDrawable(this.mThumbnailManager.get(job.getThumbnail()));
    jobIdTV.setText("" + job.getId());
    jobInputfileTV.setText("" + job.getInputFilename());

    for (final BitcodinJob.Source mSource : job.getSources()) {
      if (mSource.getType() != BitcodinJob.Source.Type.OTHER) {
        sourceWrapper.addView(createSourceView(job, mSource, sourceWrapper));
      }
    }

    jobWrapper.setOnClickListener(new OnJobItemClickListener());

    return jobWrapper;
  }

  private View createSourceView(BitcodinJob job, BitcodinJob.Source src, LinearLayout wrapper) {
    View sourceView = this.mLayoutInflater.inflate(R.layout.source_item, wrapper, false);

    TextView srcType = (TextView) sourceView.findViewById(R.id.typeTV);
    TextView srcUrl = (TextView) sourceView.findViewById(R.id.urlTV);

    if (src.getTypeStr() == "HLS") {
      srcType.setText(src.getTypeStr() + " (video only)");
    } else {
      srcType.setText(src.getTypeStr());
    }
    srcUrl.setText(src.getSrcUrl());

    sourceView.setOnClickListener(new OnSourceItemClickListener(job, src, this.listeners));

    return sourceView;
  }

  @Override
  public void onProgressChanged(double progress) {
    if (this.listeners != null) {
      for (JobManagerListener listener : this.listeners) {
        listener.onProgressChanged(progress);
      }
    }
  }
}
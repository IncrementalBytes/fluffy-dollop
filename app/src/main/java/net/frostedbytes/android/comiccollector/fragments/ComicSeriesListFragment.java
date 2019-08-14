package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

public class ComicSeriesListFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicSeriesListFragment";

  public interface OnComicSeriesListListener {

    void onSeriesListAddBook();

    void onSeriesListItemSelected(ComicSeries comicSeries);

    void onSeriesListPopulated(int size);
  }

  private OnComicSeriesListListener mCallback;

  private RecyclerView mRecyclerView;

  private HashMap<String, ComicPublisher> mComicPublishers;
  private HashMap<String, ComicSeries> mComicSeries;

  public static ComicSeriesListFragment newInstance(HashMap<String, ComicPublisher> publishers, HashMap<String, ComicSeries> series) {

    LogUtils.debug(TAG, "++newInstance(%d, %d)", publishers.size(), series.size());
    ComicSeriesListFragment fragment = new ComicSeriesListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_PUBLISHERS, publishers);
    args.putSerializable(BaseActivity.ARG_COMIC_SERIES, series);
    fragment.setArguments(args);
    return fragment;
  }

  /*
    Fragment Override(s)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnComicSeriesListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicPublishers = (HashMap<String, ComicPublisher>) arguments.getSerializable(BaseActivity.ARG_COMIC_PUBLISHERS);
      mComicSeries = (HashMap<String, ComicSeries>) arguments.getSerializable(BaseActivity.ARG_COMIC_SERIES);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_comic_series_list, container, false);

    FloatingActionButton addButton = view.findViewById(R.id.series_fab_add);
    mRecyclerView = view.findViewById(R.id.series_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);
    addButton.setOnClickListener(pickView -> mCallback.onSeriesListAddBook());

    updateUI();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mComicSeries = null;
    mComicPublishers = null;
  }

  /*
    Private Method(s)
   */
  private void updateUI() {

    if (mComicSeries == null || mComicSeries.size() == 0) {
      mCallback.onSeriesListPopulated(0);
    } else {
      LogUtils.debug(TAG, "++updateUI()");
      ArrayList<ComicSeries> comicSeries = new ArrayList<>();
      for (ComicSeries series : mComicSeries.values()) {
        if (series.ComicBooks.size() > 0) {
          comicSeries.add(series);
        }
      }

      comicSeries.sort(new SortUtils.ByNumberOfIssues());
      ComicSeriesAdapter seriesAdapter = new ComicSeriesAdapter(comicSeries);
      mRecyclerView.setAdapter(seriesAdapter);
      mCallback.onSeriesListPopulated(seriesAdapter.getItemCount());
    }
  }

  /**
   * Adapter class for ComicSeries objects
   */
  private class ComicSeriesAdapter extends RecyclerView.Adapter<SeriesHolder> {

    private final List<ComicSeries> mComicSeries;

    ComicSeriesAdapter(List<ComicSeries> comicSeries) {

      mComicSeries = comicSeries;
    }

    @NonNull
    @Override
    public SeriesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new SeriesHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesHolder holder, int position) {

      ComicSeries comicSeries = mComicSeries.get(position);
      holder.bind(comicSeries);
    }

    @Override
    public int getItemCount() {
      return mComicSeries.size();
    }
  }

  /**
   * Holder class for Series objects
   */
  private class SeriesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mIssueTextView;
    private final TextView mPublisherTextView;
    private final TextView mSeriesNameTextView;
    private final TextView mVolumeTextView;

    private ComicSeries mSeries;

    SeriesHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.series_item, parent, false));

      mIssueTextView = itemView.findViewById(R.id.series_item_text_issue_value);
      mPublisherTextView = itemView.findViewById(R.id.series_item_text_publisher);
      mSeriesNameTextView = itemView.findViewById(R.id.series_item_text_series);
      mVolumeTextView = itemView.findViewById(R.id.series_item_text_volume_value);

      itemView.setOnClickListener(this);
    }

    void bind(ComicSeries comicSeries) {

      mSeries = comicSeries;
      if (mComicPublishers != null) {
        ComicPublisher publisher = mComicPublishers.get(mSeries.PublisherId);
        if (publisher != null) {
          mPublisherTextView.setText(publisher.Name);
        } else {
          mPublisherTextView.setText("N/A");
        }
      } else {
        mPublisherTextView.setText("N/A");
      }

      if (mComicSeries != null) {
        ComicSeries series = mComicSeries.get(mSeries.getProductId());
        if (series != null) {
          mSeriesNameTextView.setText(String.format(Locale.US, "%s%s", series.SeriesName, series.IsFlagged ? " (pending)" : ""));
          mVolumeTextView.setText(String.valueOf(series.Volume));
        } else {
          mSeriesNameTextView.setText("N/A");
          mVolumeTextView.setText("N/A");
        }
      } else {
        mSeriesNameTextView.setText("N/A");
        mVolumeTextView.setText("N/A");
      }

      mIssueTextView.setText(String.valueOf(mSeries.ComicBooks.size()));
    }

    @Override
    public void onClick(View view) {

      LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
      mCallback.onSeriesListItemSelected(mSeries);
    }
  }
}

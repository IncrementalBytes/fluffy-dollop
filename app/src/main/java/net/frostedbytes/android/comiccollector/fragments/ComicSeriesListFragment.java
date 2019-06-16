package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

  private static final String TAG = BASE_TAG + ComicSeriesListFragment.class.getSimpleName();

  public interface OnComicSeriesListListener {

    void onComicSeriesListAdd();

    void onComicSeriesListItemSelected(ComicSeries comicSeries);

    void onComicSeriesListPopulated(int size);

    void onComicSeriesListSynchronize();
  }

  private OnComicSeriesListListener mCallback;

  private RecyclerView mRecyclerView;

  private HashMap<String, ComicPublisher> mComicPublishers;
  private ArrayList<ComicSeries> mComicSeries;

  public static ComicSeriesListFragment newInstance(ArrayList<ComicSeries> comicSeries, HashMap<String, ComicPublisher> publishers) {

    LogUtils.debug(TAG, "++newInstance(%d)", comicSeries.size());
    ComicSeriesListFragment fragment = new ComicSeriesListFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(BaseActivity.ARG_COMIC_SERIES_LIST, comicSeries);
    args.putSerializable(BaseActivity.ARG_COMIC_PUBLISHERS, publishers);
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
      mComicSeries = arguments.getParcelableArrayList(BaseActivity.ARG_COMIC_SERIES_LIST);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_comic_series_list, container, false);

    FloatingActionButton mAddButton = view.findViewById(R.id.series_fab_add);
    mRecyclerView = view.findViewById(R.id.series_list_view);
    FloatingActionButton mSyncButton = view.findViewById(R.id.series_fab_sync);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);

    mAddButton.setOnClickListener(pickView -> mCallback.onComicSeriesListAdd());
    mSyncButton.setOnClickListener(pickView -> mCallback.onComicSeriesListSynchronize());

    updateUI();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mComicSeries = null;
  }

  /*
    Private Method(s)
   */
  private void updateUI() {

    if (mComicSeries == null || mComicSeries.size() == 0) {
      mCallback.onComicSeriesListPopulated(0);
    } else {
      LogUtils.debug(TAG, "++updateUI()");
      mComicSeries.sort(new SortUtils.BySeriesName());
      ComicSeriesAdapter comicAdapter = new ComicSeriesAdapter(mComicSeries);
      mRecyclerView.setAdapter(comicAdapter);
      mCallback.onComicSeriesListPopulated(comicAdapter.getItemCount());
    }
  }

  /**
   * Adapter class for ComicSeries objects
   */
  private class ComicSeriesAdapter extends RecyclerView.Adapter<ComicHolder> {

    private final List<ComicSeries> mComicSeries;

    ComicSeriesAdapter(List<ComicSeries> comicSeries) {

      mComicSeries = comicSeries;
    }

    @NonNull
    @Override
    public ComicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new ComicHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicHolder holder, int position) {

      ComicSeries comicSeries = mComicSeries.get(position);
      holder.bind(comicSeries);
    }

    @Override
    public int getItemCount() {
      return mComicSeries.size();
    }
  }

  /**
   * Holder class for Comic Series objects
   */
  private class ComicHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mPublisherTextView;
    private final TextView mSeriesNameTextView;
    private final TextView mVolumeTextView;

    private ComicSeries mComicSeries;

    ComicHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_series_item, parent, false));

      mPublisherTextView = itemView.findViewById(R.id.comic_series_text_publisher);
      mSeriesNameTextView = itemView.findViewById(R.id.comic_series_text_name);
      mVolumeTextView = itemView.findViewById(R.id.comic_series_text_volume_value);

      itemView.setOnClickListener(this);
    }

    void bind(ComicSeries comicSeries) {

      mComicSeries = comicSeries;

      if (mComicPublishers != null) {
        ComicPublisher publisher = mComicPublishers.get(mComicSeries.PublisherId);
        if (publisher != null) {
          mPublisherTextView.setText(publisher.Name);
        } else {
          mPublisherTextView.setText("N/A");
        }
      } else {
        mPublisherTextView.setText("N/A");
      }

      mSeriesNameTextView.setText(mComicSeries.SeriesName);
      mVolumeTextView.setText(String.valueOf(mComicSeries.Volume));
    }

    @Override
    public void onClick(View view) {

      LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
      mCallback.onComicSeriesListItemSelected(mComicSeries);
    }
  }
}

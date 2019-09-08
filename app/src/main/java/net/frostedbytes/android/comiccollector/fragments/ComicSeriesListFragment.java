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
import androidx.lifecycle.ViewModelProviders;
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
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;
import net.frostedbytes.android.comiccollector.viewmodel.CollectorViewModel;

public class ComicSeriesListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicSeriesListFragment";

  public interface OnComicSeriesListListener {

    void onSeriesListAddBook();

    void onSeriesListItemSelected(ComicSeriesDetails comicSeries);

    void onSeriesListOnPopulated(int size);
  }

  private OnComicSeriesListListener mCallback;

  private RecyclerView mRecyclerView;

  private HashMap<String, ComicSeriesDetails> mComicSeries;

  public static ComicSeriesListFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new ComicSeriesListFragment();
  }

  /*
    Fragment Override(s)
   */
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
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    mComicSeries = new HashMap<>();
    CollectorViewModel collectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
    collectorViewModel.getComicBooks().observe(this, comicList -> {

      if (comicList.size() > 0) {
        for (ComicBookDetails comicBookDetail : comicList) {
          ComicSeriesDetails tempSeries = mComicSeries.get(comicBookDetail.ProductCode);
          if (tempSeries == null) {
            tempSeries = new ComicSeriesDetails();
            tempSeries.Id = comicBookDetail.ProductCode;
            tempSeries.OwnedIssues.add(comicBookDetail.IssueNumber);
            tempSeries.PublisherName = comicBookDetail.PublisherName;
            tempSeries.Title = comicBookDetail.SeriesTitle;
            tempSeries.Volume = comicBookDetail.Volume;
          }

          if (!tempSeries.OwnedIssues.contains(comicBookDetail.IssueNumber)) {
            tempSeries.OwnedIssues.add(comicBookDetail.IssueNumber);
          }

          int year = Integer.parseInt(comicBookDetail.Published.substring(3));
          if (!tempSeries.Published.contains(year)) {
            tempSeries.Published.add(year);
          }

          tempSeries.Published.add(Integer.parseInt(comicBookDetail.Published.substring(3)));

          if (mComicSeries.containsKey(comicBookDetail.ProductCode)) {
            mComicSeries.replace(comicBookDetail.ProductCode, tempSeries);
          } else {
            mComicSeries.put(comicBookDetail.ProductCode, tempSeries);
          }
        }

        LogUtils.debug(TAG, "Found %d comic series from %d comic books.", mComicSeries.size(), comicList.size());
        ComicSeriesAdapter comicAdapter = new ComicSeriesAdapter(new ArrayList<>(mComicSeries.values()));
        mRecyclerView.setAdapter(comicAdapter);
        mCallback.onSeriesListOnPopulated(mComicSeries.size());
      } else {
        mCallback.onSeriesListOnPopulated(0);
      }
    });

    return inflater.inflate(R.layout.fragment_comic_series_list, container, false);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
  }

  @Override
  public void onDetach() {
    super.onDetach();

    LogUtils.debug(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    LogUtils.debug(TAG, "++onViewCreated(View, Bundle)");
    FloatingActionButton addButton = view.findViewById(R.id.series_fab_add);
    mRecyclerView = view.findViewById(R.id.series_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);
    addButton.setOnClickListener(pickView -> mCallback.onSeriesListAddBook());
  }

  /**
   * Adapter class for ComicSeries objects
   */
  private class ComicSeriesAdapter extends RecyclerView.Adapter<SeriesHolder> {

    private final List<ComicSeriesDetails> mComicSeries;

    ComicSeriesAdapter(List<ComicSeriesDetails> comicSeries) {

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

      ComicSeriesDetails comicSeries = mComicSeries.get(position);
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
    private final TextView mPublishedTextView;

    private ComicSeriesDetails mSeries;

    SeriesHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_series_item, parent, false));

      mIssueTextView = itemView.findViewById(R.id.series_item_text_issue_value);
      mPublisherTextView = itemView.findViewById(R.id.series_item_text_publisher);
      mSeriesNameTextView = itemView.findViewById(R.id.series_item_text_series);
      mPublishedTextView = itemView.findViewById(R.id.series_item_text_published_value);

      itemView.setOnClickListener(this);
    }

    void bind(ComicSeriesDetails comicSeries) {

      mSeries = comicSeries;
      mPublisherTextView.setText(mSeries.PublisherName);
      mSeriesNameTextView.setText(mSeries.Title);
      mSeries.Published.sort(new SortUtils.ByYearAscending());
      mPublishedTextView.setText(
        String.format(
          Locale.US,
          "%d-%d",
          mSeries.Published.get(0), mSeries.Published.get(mSeries.Published.size() -1)));
      mIssueTextView.setText(String.valueOf(mSeries.OwnedIssues.size()));
    }

    @Override
    public void onClick(View view) {

      LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
      mCallback.onSeriesListItemSelected(mSeries);
    }
  }
}

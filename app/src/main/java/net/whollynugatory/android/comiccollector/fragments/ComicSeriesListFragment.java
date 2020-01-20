/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.whollynugatory.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.SortUtils;
import net.whollynugatory.android.comiccollector.db.viewmodel.SeriesViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicSeriesDetails;

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

    Log.d(TAG, "++newInstance()");
    return new ComicSeriesListFragment();
  }

  /*
    Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnComicSeriesListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    mComicSeries = new HashMap<>();
    SeriesViewModel seriesViewModel = ViewModelProviders.of(this).get(SeriesViewModel.class);
    seriesViewModel.getAll().observe(this, comicList -> {

      if (comicList.size() > 0) {
//        for (ComicBookDetails comicBookDetail : comicList) {
//          ComicSeriesDetails tempSeries = mComicSeries.get(comicBookDetail.ProductCode);
//          if (tempSeries == null) {
//            tempSeries = new ComicSeriesDetails();
//            tempSeries.Id = comicBookDetail.ProductCode;
//            tempSeries.OwnedIssues.add(comicBookDetail.IssueNumber);
//            tempSeries.PublisherName = comicBookDetail.PublisherName;
//            tempSeries.Title = comicBookDetail.SeriesTitle;
//            tempSeries.Volume = comicBookDetail.Volume;
//          }
//
//          if (!tempSeries.OwnedIssues.contains(comicBookDetail.IssueNumber)) {
//            tempSeries.OwnedIssues.add(comicBookDetail.IssueNumber);
//          }
//
//          int year = Integer.parseInt(comicBookDetail.Published.substring(3));
//          if (!tempSeries.Published.contains(year)) {
//            tempSeries.Published.add(year);
//          }
//
//          tempSeries.Published.add(Integer.parseInt(comicBookDetail.Published.substring(3)));
//
//          if (mComicSeries.containsKey(comicBookDetail.ProductCode)) {
//            mComicSeries.replace(comicBookDetail.ProductCode, tempSeries);
//          } else {
//            mComicSeries.put(comicBookDetail.ProductCode, tempSeries);
//          }
//        }

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

    Log.d(TAG, "++onDestroy()");
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
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

      Log.d(TAG, "++ComicHolder::onClick(View)");
      mCallback.onSeriesListItemSelected(mSeries);
    }
  }
}

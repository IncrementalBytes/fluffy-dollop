/*
 * Copyright 2020 Ryan Ward
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

package net.whollynugatory.android.comiccollector.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class SeriesListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesListFragment";

  public interface OnSeriesListListener {

    void onSeriesListAddComicBook();

    void onSeriesListSelected(String series);

    void onSeriesListPopulated(int size);
  }

  private OnSeriesListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private CollectorViewModel mCollectorViewModel;

  public static SeriesListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    SeriesListFragment fragment = new SeriesListFragment();
    Bundle arguments = new Bundle();
    fragment.setArguments(arguments);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    SeriesAdapter seriesAdapter = new SeriesAdapter(getContext());
    mRecyclerView.setAdapter(seriesAdapter);
    mCollectorViewModel.getRecentSeries().observe(getViewLifecycleOwner(), seriesAdapter::setSeriesDetailList);

    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onSeriesListAddComicBook());
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnSeriesListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_series_list, container, false);
    mAddButton = view.findViewById(R.id.series_fab_add);
    mRecyclerView = view.findViewById(R.id.series_list_view);
    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
  }

  /*
    Adapter class for SeriesAdapter objects
  */
  private class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesHolder> {

    /*
      Holder class for SeriesDetails objects
     */
    class SeriesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final TextView mCountTextView;
      private final TextView mPublishedTextView;
      private final TextView mPublisherTextView;
      private final TextView mTitleTextView;

      private SeriesEntity mSeriesEntity;

      SeriesHolder(View itemView) {
        super(itemView);

        mCountTextView = itemView.findViewById(R.id.series_item_text_issue_value);
        mPublishedTextView = itemView.findViewById(R.id.series_item_text_published_value);
        mPublisherTextView =  itemView.findViewById(R.id.series_item_text_publisher);
        mTitleTextView = itemView.findViewById(R.id.series_item_text_title);

        itemView.setOnClickListener(this);
      }

      void bind(SeriesEntity seriesEntity) {

        mSeriesEntity = seriesEntity;

        if (mSeriesEntity != null) {
//          mCountTextView.setText(String.valueOf(mSeriesEntity.OwnedIssues));
          String publishedValue = "TBD";
          mPublishedTextView.setText(publishedValue);
          mPublisherTextView.setText(mSeriesEntity.Publisher);
          mTitleTextView.setText(mSeriesEntity.Name);
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++BookAuthorHolder::onClick(View)");
        mCallback.onSeriesListSelected(mSeriesEntity.Id);
      }
    }

    private final LayoutInflater mInflater;
    private List<SeriesEntity> mSeriesEntityList;

    SeriesAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public SeriesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_series, parent, false);
      return new SeriesHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesHolder holder, int position) {

      if (mSeriesEntityList != null) {
        SeriesEntity seriesEntity = mSeriesEntityList.get(position);
        holder.bind(seriesEntity);
      } else {
        // No Series!
      }
    }

    @Override
    public int getItemCount() {

      if (mSeriesEntityList != null) {
        return mSeriesEntityList.size();
      } else {
        return 0;
      }
    }

    void setSeriesDetailList(List<SeriesEntity> seriesEntityList) {

      Log.d(TAG, "++setSeriesDetailList(List<SeriesEntity>)");
      mSeriesEntityList = seriesEntityList;
      notifyDataSetChanged();
    }
  }
}

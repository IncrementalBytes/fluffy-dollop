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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
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
import net.whollynugatory.android.comiccollector.common.SortUtils.ByNumberOfIssues;
import net.whollynugatory.android.comiccollector.common.SortUtils.BySeriesTitle;
import net.whollynugatory.android.comiccollector.common.SortUtils.ByVolume;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
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
  private Spinner mSortSpinner;

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
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onSeriesListAddComicBook());

    updateUI();
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
    mAddButton = view.findViewById(R.id.series_list_fab_add);
    mRecyclerView = view.findViewById(R.id.series_list_view);
    mSortSpinner = view.findViewById(R.id.series_list_spinner_sort);

    mSortSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateUI();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

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

  private void updateUI() {

    SeriesAdapter seriesAdapter = new SeriesAdapter(getContext());
    mRecyclerView.setAdapter(seriesAdapter);
    mCollectorViewModel.getSeries().observe(getViewLifecycleOwner(), seriesAdapter::setSeriesDetailList);
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
      private final TextView mPublisherTextView;
      private final TextView mTitleTextView;
      private final TextView mVolumeTextView;

      private SeriesDetails mSeriesDetails;

      SeriesHolder(View itemView) {
        super(itemView);

        mCountTextView = itemView.findViewById(R.id.series_item_text_issue_value);
        mPublisherTextView =  itemView.findViewById(R.id.series_item_text_publisher);
        mTitleTextView = itemView.findViewById(R.id.series_item_text_title);
        mVolumeTextView = itemView.findViewById(R.id.series_item_text_volume_value);

        itemView.setOnClickListener(this);
      }

      void bind(SeriesDetails seriesDetails) {

        mSeriesDetails = seriesDetails;

        if (mSeriesDetails != null) {
          mCountTextView.setText(String.valueOf(mSeriesDetails.BookCount));
          mPublisherTextView.setText(mSeriesDetails.Publisher);
          mTitleTextView.setText(mSeriesDetails.SeriesTitle);
          mVolumeTextView.setText(String.valueOf(mSeriesDetails.Volume));
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++BookAuthorHolder::onClick(View)");
        mCallback.onSeriesListSelected(mSeriesDetails.SeriesId);
      }
    }

    private final LayoutInflater mInflater;
    private List<SeriesDetails> mSeriesDetailsList;

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

      if (mSeriesDetailsList != null) {
        SeriesDetails seriesDetails = mSeriesDetailsList.get(position);
        holder.bind(seriesDetails);
      } else {
        // No Series!
      }
    }

    @Override
    public int getItemCount() {

      if (mSeriesDetailsList != null) {
        return mSeriesDetailsList.size();
      } else {
        return 0;
      }
    }

    void setSeriesDetailList(List<SeriesDetails> seriesDetailList) {

      Log.d(TAG, "++setSeriesDetailList(List<SeriesDetails>)");
      mSeriesDetailsList = seriesDetailList;
      switch (mSortSpinner.getSelectedItemPosition()) {
        case 1:
          mSeriesDetailsList.sort(new ByVolume());
          break;
        case 2:
          mSeriesDetailsList.sort(new ByNumberOfIssues());
          break;
        default:
          mSeriesDetailsList.sort(new BySeriesTitle());
          break;
      }

      mCallback.onSeriesListPopulated(seriesDetailList.size());
      notifyDataSetChanged();
    }
  }
}

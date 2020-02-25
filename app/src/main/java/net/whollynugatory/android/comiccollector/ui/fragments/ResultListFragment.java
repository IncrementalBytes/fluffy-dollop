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
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ResultListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + ResultListFragment.class.getSimpleName();

  public interface OnResultListListener {

    void onResultListActionComplete(String message);

    void onResultListItemSelected(ComicDetails comicDetails);
  }

  private OnResultListListener mCallback;

  private RecyclerView mRecyclerView;

  private CollectorViewModel mCollectorViewModel;
  private ArrayList<ComicDetails> mComicDetailsList;

  public static ResultListFragment newInstance(ArrayList<ComicDetails> comicDetailsList) {

    Log.d(TAG, "++newInstance(ArrayList<ComicBookEntity>)");
    ResultListFragment fragment = new ResultListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_LIST_BOOK, comicDetailsList);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    mCollectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnResultListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicDetailsList = (ArrayList<ComicDetails>)arguments.getSerializable(BaseActivity.ARG_LIST_BOOK);
    } else {
      String message = "Arguments were null.";
      Log.e(TAG, message);
      mCallback.onResultListActionComplete(message);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_result_list, container, false);

    mRecyclerView = view.findViewById(R.id.result_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);

    updateUI();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    mComicDetailsList = null;
  }

  /*
      Private Method(s)
   */
  private void updateUI() {

    if (mComicDetailsList == null || mComicDetailsList.size() == 0) {
      Log.w(TAG, "No results found.");
    } else {
      Log.d(TAG, "++updateUI()");
      ResultAdapter resultAdapter = new ResultAdapter(mComicDetailsList);
      mRecyclerView.setAdapter(resultAdapter);
    }
  }

  /**
   * Adapter class for query result objects
   */
  private class ResultAdapter extends RecyclerView.Adapter<ResultHolder> {

    private final List<ComicDetails> mComicDetailsList;

    ResultAdapter(List<ComicDetails> comicDetailsList) {

      mComicDetailsList = comicDetailsList;
    }

    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new ResultHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultHolder holder, int position) {

      ComicDetails comicDetails = mComicDetailsList.get(position);
      holder.bind(comicDetails);
    }

    @Override
    public int getItemCount() {
      return mComicDetailsList.size();
    }
  }

  /**
   * Holder class for query result object
   */
  private class ResultHolder extends RecyclerView.ViewHolder {

    private final TextView mIssueTextView;
    private final TextView mPublishedTextView;
    private final TextView mPublisherTextView;
    private final TextView mSeriesTextView;
    private final TextView mTitleTextView;

    private ComicDetails mComicDetails;

    ResultHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.item_result, parent, false));

      Button addButton = itemView.findViewById(R.id.result_button_add);
      mIssueTextView = itemView.findViewById(R.id.result_text_issue);
      mPublishedTextView = itemView.findViewById(R.id.result_text_published);
      mPublisherTextView = itemView.findViewById(R.id.result_text_publisher);
      mSeriesTextView = itemView.findViewById(R.id.result_text_series);
      mTitleTextView = itemView.findViewById(R.id.result_text_title);

      addButton.setOnClickListener(v -> {

        Log.d(TAG, "++ResultHolder::onClick(View)");
        mComicDetails.AddedDate = mComicDetails.UpdatedDate = Calendar.getInstance().getTimeInMillis();

        mCollectorViewModel.insertComicBook(mComicDetails.toEntity());
        mCallback.onResultListItemSelected(mComicDetails);
      });
    }

    void bind(ComicDetails comicDetails) {

      mComicDetails = comicDetails;

      mSeriesTextView.setText(mComicDetails.SeriesTitle);
      mIssueTextView.setText(mComicDetails.IssueCode);
      mPublishedTextView.setText(String.format(Locale.US, getString(R.string.published_date_format), mComicDetails.Published));
      mPublisherTextView.setText(String.format(Locale.US, getString(R.string.publisher_format), mComicDetails.Publisher));
      mTitleTextView.setText(mComicDetails.Title);
    }
  }
}

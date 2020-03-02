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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ComicListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicListFragment";

  public interface OnComicListListener {

    void onComicListAddComicBook();

    void onComicListEditComicBook(ComicDetails comicDetails);

    void onComicListPopulated(int size);
  }

  private OnComicListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private CollectorViewModel mCollectorViewModel;

  private ComicDetails mComicDetails;
  private String mSeriesId;

  public static ComicListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    ComicListFragment fragment = new ComicListFragment();
    Bundle arguments = new Bundle();
    fragment.setArguments(arguments);
    return fragment;
  }

  public static ComicListFragment newInstance(ComicDetails comicDetails) {

    Log.d(TAG, "++newInstance(ComicDetails)");
    ComicListFragment fragment = new ComicListFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicDetails);
    fragment.setArguments(arguments);
    return fragment;
  }

  public static ComicListFragment newInstance(String seriesId) {

    Log.d(TAG, "++newInstance(String)");
    ComicListFragment fragment = new ComicListFragment();
    Bundle arguments = new Bundle();
    arguments.putString(BaseActivity.ARG_SERIES_ID, seriesId);
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
    ComicDetailsAdapter comicDetailsAdapter = new ComicDetailsAdapter(getContext());
    mRecyclerView.setAdapter(comicDetailsAdapter);
    if (mSeriesId != null && !mSeriesId.isEmpty()) {
      mCollectorViewModel.getComicsBySeriesId(mSeriesId).observe(getViewLifecycleOwner(), comicDetailsAdapter::setComicDetailsList);
    } else if (mComicDetails == null) {
      mCollectorViewModel.getRecentComics().observe(getViewLifecycleOwner(), comicDetailsAdapter::setComicDetailsList);
    } else { // TODO: only show passed comic book in list
      comicDetailsAdapter.setComicDetailsList(Collections.singletonList(mComicDetails));
    }

    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onComicListAddComicBook());
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnComicListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    mComicDetails = null;
    mSeriesId = null;
    Bundle arguments = getArguments();
    if (arguments != null) {
      if (arguments.containsKey(BaseActivity.ARG_COMIC_BOOK)) {
        mComicDetails = (ComicDetails) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
      }

      if (arguments.containsKey(BaseActivity.ARG_SERIES_ID)) {
        mSeriesId = arguments.getString(BaseActivity.ARG_SERIES_ID);
      }
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_list, container, false);
    mAddButton = view.findViewById(R.id.fab_add);
    mRecyclerView = view.findViewById(R.id.list_view);
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
  Adapter class for ComicDetails objects
 */
  private class ComicDetailsAdapter extends RecyclerView.Adapter<ComicDetailsAdapter.ComicDetailsHolder> {

    /*
      Holder class for ComicDetails objects
     */
    class ComicDetailsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final ImageView mDeleteImage;
      private final TextView mIssueTextView;
      private final CheckBox mOwnCheck;
      private final CheckBox mReadCheck;
      private final TextView mSeriesNameTextView;
      private final TextView mTitleTextView;

      private ComicDetails mComicBook;

      ComicDetailsHolder(View itemView) {
        super(itemView);

        mDeleteImage = itemView.findViewById(R.id.comic_item_image_delete);
        mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
        mOwnCheck = itemView.findViewById(R.id.comic_item_check_own);
        mReadCheck = itemView.findViewById(R.id.comic_item_check_read);
        mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
        mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);

        itemView.setOnClickListener(this);
      }

      void bind(ComicDetails comicDetails) {

        mComicBook = comicDetails;

        if (mComicBook != null) {
          mDeleteImage.setOnClickListener(v -> {
            if (getActivity() != null) {
              String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mComicBook.Title);
              AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mCollectorViewModel.deleteComicById(mComicBook.Id))
                .setNegativeButton(android.R.string.no, null)
                .create();
              removeBookDialog.show();
            } else {
              Log.w(TAG, "Unable to remove book at this time.");
            }
          });

          mOwnCheck.setChecked(mComicBook.IsOwned);
          mReadCheck.setChecked(mComicBook.HasRead);
          mSeriesNameTextView.setText(mComicBook.SeriesTitle);
          mTitleTextView.setText(mComicBook.Title);
          mIssueTextView.setText(String.valueOf(mComicBook.getIssueNumber()));
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++ComicDetailsHolder::onClick(View)");
        mCallback.onComicListEditComicBook(mComicBook);
      }
    }

    private final LayoutInflater mInflater;
    private List<ComicDetails> mComicDetailsList;

    ComicDetailsAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ComicDetailsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_comic_book, parent, false);
      return new ComicDetailsHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicDetailsHolder holder, int position) {

      if (mComicDetailsList != null) {
        ComicDetails comicDetails = mComicDetailsList.get(position);
        holder.bind(comicDetails);
      } else {
        // No books!
      }
    }

    @Override
    public int getItemCount() {

      if (mComicDetailsList != null) {
        return mComicDetailsList.size();
      } else {
        return 0;
      }
    }

    void setComicDetailsList(List<ComicDetails> comicDetailsList) {

      Log.d(TAG, "++setComicDetailsList(List<ComicDetails>)");
      mComicDetailsList = new ArrayList<>(comicDetailsList);
      mCallback.onComicListPopulated(comicDetailsList.size());
      notifyDataSetChanged();
    }
  }
}

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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

import java.util.Locale;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.ComicBookViewModel;

public class ComicBookListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookListFragment";

  public interface OnComicBookListListener {

    void onComicListActionComplete(String message);

    void onComicListAddBook();

    void onComicListDeleteBook();

    void onComicListItemSelected(ComicBookEntity comicBook);

    void onComicListPopulated(int size);
  }

  private OnComicBookListListener mCallback;

  private ComicBookViewModel mComicBookViewModel;

  private RecyclerView mRecyclerView;

  private String mProductCode;

  public static ComicBookListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new ComicBookListFragment();
  }

  public static ComicBookListFragment newInstance(String productCode) {

    Log.d(TAG, "++newInstance(String)");
    ComicBookListFragment fragment = new ComicBookListFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_PRODUCT_CODE, productCode);
    fragment.setArguments(args);
    return fragment;
  }
  /*
    Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnComicBookListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mProductCode = arguments.getString(BaseActivity.ARG_PRODUCT_CODE);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    mComicBookViewModel = ViewModelProviders.of(this).get(ComicBookViewModel.class);
    if (mProductCode != null && mProductCode.length() > 0) {
//      mComicBookViewModel.getComicBooksByProductCode(mProductCode).observe(this, bookList -> {
//
//        ComicBookAdapter comicAdapter = new ComicBookAdapter(bookList);
//        mRecyclerView.setAdapter(comicAdapter);
//        mCallback.onComicListPopulated(bookList.size());
//      });
    } else {
      mComicBookViewModel.getRecent().observe(this, bookList -> {

        ComicBookAdapter comicAdapter = new ComicBookAdapter(bookList);
        mRecyclerView.setAdapter(comicAdapter);
        mCallback.onComicListPopulated(bookList.size());
      });
    }

    return inflater.inflate(R.layout.fragment_comic_book_list, container, false);
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
    FloatingActionButton addButton = view.findViewById(R.id.comic_fab_add);
    mRecyclerView = view.findViewById(R.id.comic_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);
    addButton.setOnClickListener(pickView -> mCallback.onComicListAddBook());
  }

  /**
   * Adapter class for ComicBook objects
   */
  private class ComicBookAdapter extends RecyclerView.Adapter<ComicHolder> {

    private final List<ComicBookEntity> mComicBooks;

    ComicBookAdapter(List<ComicBookEntity> comicBooks) {

      mComicBooks = comicBooks;
    }

    @NonNull
    @Override
    public ComicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new ComicHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicHolder holder, int position) {

      ComicBookEntity comicBook = mComicBooks.get(position);
      holder.bind(comicBook);
    }

    @Override
    public int getItemCount() {
      return mComicBooks.size();
    }
  }

  /**
   * Holder class for Comic objects
   */
  private class ComicHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final ImageView mDeleteImage;
    private final TextView mIssueTextView;
    private final ImageView mOwnImage;
    private final ImageView mReadImage;
    private final TextView mSeriesNameTextView;
    private final TextView mTitleTextView;

    private ComicBookEntity mComicBook;

    ComicHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_book_item, parent, false));

      mDeleteImage = itemView.findViewById(R.id.comic_item_image_delete);
      mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
      mOwnImage = itemView.findViewById(R.id.comic_item_image_own);
      mReadImage = itemView.findViewById(R.id.comic_item_image_read);
      mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
      mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);

      itemView.setOnClickListener(this);
    }

    void bind(ComicBookEntity comicBook) {

      mComicBook = comicBook;

      mDeleteImage.setOnClickListener(v -> {
        if (getActivity() != null) {
          String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mComicBook.Title);
          if (mComicBook.Title.isEmpty()) {
            message = getString(R.string.remove_book_message);
          }

          AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
              mComicBookViewModel.delete(mComicBook.Id);
              mCallback.onComicListDeleteBook();
            })
            .setNegativeButton(android.R.string.no, null)
            .create();
          removeBookDialog.show();
        } else {
          mCallback.onComicListActionComplete(getString(R.string.err_remove_comic_book));
        }
      });

      if (mComicBook.IsOwned) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      if (mComicBook.IsRead) {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

//      mSeriesNameTextView.setText(mComicBook.SeriesTitle);
      mTitleTextView.setText(mComicBook.Title);
//      mIssueTextView.setText(String.valueOf(mComicBook.IssueNumber));
    }

    @Override
    public void onClick(View view) {

      Log.d(TAG, "++ComicHolder::onClick(View)");
      mCallback.onComicListItemSelected(mComicBook);
    }
  }
}

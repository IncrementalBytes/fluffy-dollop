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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

import java.util.Locale;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.views.ComicBookDetails;

public class ComicBookFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookFragment";

  public interface OnComicBookListener {

    void onComicBookActionComplete(String message);

    void onComicBookAddedToLibrary(ComicBookEntity comicBookEntity);

    void onComicBookInit(boolean isSuccessful);
  }

  private OnComicBookListener mCallback;

  private TextView mSeriesText;
  private TextView mPublisherText;
  private EditText mPublishedDateEdit;
  private TextView mVolumeText;
  private TextView mIssueText;
  private TextView mProductCodeText;
  private ToggleButton mOwnedToggle;
  private ToggleButton mReadToggle;
  private EditText mTitleEdit;
  private Button mSaveButton;

  private ComicBookEntity mComicBook;

  public static ComicBookFragment newInstance(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++newInstance(ComicBookDetails)");
    ComicBookFragment fragment = new ComicBookFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicBookEntity);
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
      mCallback = (OnComicBookListener) context;
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
      mComicBook = (ComicBookEntity) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_comic_book, container, false);
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
    mTitleEdit = view.findViewById(R.id.comic_book_edit_title);
    mSeriesText = view.findViewById(R.id.comic_book_text_series_value);
    mPublisherText = view.findViewById(R.id.comic_book_text_publisher_value);
    mPublishedDateEdit = view.findViewById(R.id.comic_book_edit_published_date);
    mVolumeText = view.findViewById(R.id.comic_book_text_volume_value);
    mIssueText = view.findViewById(R.id.comic_book_text_issue_value);
    mProductCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
    mOwnedToggle = view.findViewById(R.id.comic_book_toggle_owned);
    mReadToggle = view.findViewById(R.id.comic_book_toggle_read);
    mSaveButton = view.findViewById(R.id.comic_book_button_save);
    updateUI(mComicBook);
  }

  /*
    Private Method(s)
   */
  private void updateUI(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++updateUI()");
    if (comicBookEntity == null) {
      mCallback.onComicBookInit(false);
    } else {
      mTitleEdit.setText(comicBookEntity.Title);
      if (!comicBookEntity.PublishedDate.equals(BaseActivity.DEFAULT_PUBLISHED_DATE) &&
        comicBookEntity.PublishedDate.length() == BaseActivity.DEFAULT_PUBLISHED_DATE.length()) {
        mPublishedDateEdit.setText(comicBookEntity.PublishedDate);
      }

      mPublishedDateEdit.addTextChangedListener(new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          validateAll();
        }
      });

      // TODO: update
//      if (comicBookEntity.PublisherName.length() > 0) {
//        mPublisherText.setText(comicBookEntity.PublisherName);
//      } else {
        mPublisherText.setText(getString(R.string.placeholder));
//      }

      // TODO: update
//      if (comicBookEntity.SeriesTitle.length() > 0) {
//        mSeriesText.setText(comicBookEntity.SeriesTitle);
//      }

      mProductCodeText.setText(comicBookEntity.ProductCode);
      mOwnedToggle.setChecked(comicBookEntity.IsOwned);
      mReadToggle.setChecked(comicBookEntity.IsRead);

      mSaveButton.setEnabled(false);
      mSaveButton.setOnClickListener(v -> {

        ComicBookEntity updatedBook = new ComicBookEntity();
        updatedBook.parseProductCode(mComicBook.Id);
        updatedBook.IsOwned = mOwnedToggle.isChecked();
        updatedBook.IsRead = mReadToggle.isChecked();
        updatedBook.Title = mTitleEdit.getText().toString();
        updatedBook.PublishedDate = mPublishedDateEdit.getText().toString();

        mCallback.onComicBookAddedToLibrary(updatedBook);
      });
    }

    validateAll();
  }

  private void validateAll() {

    if (mPublishedDateEdit.getText().toString().length() == BaseActivity.DEFAULT_PUBLISHED_DATE.length() &&
      !mPublishedDateEdit.getText().toString().equals(BaseActivity.DEFAULT_PUBLISHED_DATE)) {
      mSaveButton.setEnabled(true);
    } else {
      mSaveButton.setEnabled(false);
    }
  }
}

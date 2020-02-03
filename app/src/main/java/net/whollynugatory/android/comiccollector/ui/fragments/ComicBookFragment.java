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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.ComicBookViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ComicBookFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookFragment";

  public interface OnComicBookListener {

    void onComicBookSaved(ComicBookEntity comicBookEntity);

    void onComicBookCancel();
  }

  private OnComicBookListener mCallback;

  private ComicBookViewModel mComicBookViewModel;

  private ComicBookEntity mComicBookEntity;

  public static ComicBookFragment newInstance(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    ComicBookFragment fragment = new ComicBookFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicBookEntity);
    fragment.setArguments(arguments);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(@NonNull Context context) {
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
      mComicBookEntity = (ComicBookEntity) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mComicBookViewModel = ViewModelProviders.of(this).get(ComicBookViewModel.class);
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

    // TODO: replace with data binding
    Button cancelButton = view.findViewById(R.id.comic_book_button_cancel);
    EditText nameEdit = view.findViewById(R.id.comic_book_edit_title);
    EditText publishEdit = view.findViewById(R.id.comic_book_edit_published_date);
    TextView seriesText = view.findViewById(R.id.comic_book_text_series_value);
    TextView publisherText = view.findViewById(R.id.comic_book_text_publisher_value);
    TextView volumeText = view.findViewById(R.id.comic_book_text_volume_value);
    TextView issueText = view.findViewById(R.id.comic_book_text_issue_value);
    TextView productCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
    ToggleButton ownedButton = view.findViewById(R.id.comic_book_toggle_owned);
    ToggleButton readButton = view.findViewById(R.id.comic_book_toggle_read);
    Button saveButton = view.findViewById(R.id.comic_book_button_save);

    nameEdit.setText(mComicBookEntity.Title);
    publishEdit.setText(mComicBookEntity.PublishedDate);
    seriesText.setText("From other object");
    publisherText.setText("From other object");
    volumeText.setText("From other object");
    issueText.setText(String.valueOf(mComicBookEntity.getIssueNumber()));
    productCodeText.setText(mComicBookEntity.Id);

    ownedButton.setChecked(mComicBookEntity.IsOwned);
    readButton.setChecked(mComicBookEntity.HasRead);

    cancelButton.setOnClickListener(v -> mCallback.onComicBookCancel());
    saveButton.setOnClickListener(v -> {

      Log.d(TAG, "++onClick()");
      ComicBookEntity comicBookEntity = new ComicBookEntity(mComicBookEntity);
      comicBookEntity.Title = nameEdit.getText().toString();
      comicBookEntity.IsOwned = ownedButton.isChecked();
      comicBookEntity.HasRead = readButton.isChecked();
      comicBookEntity.PublishedDate = publishEdit.getText().toString();

      mComicBookViewModel.insert(comicBookEntity);
      mCallback.onComicBookSaved(comicBookEntity);
    });
  }
}

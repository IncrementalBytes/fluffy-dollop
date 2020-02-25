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
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ComicBookFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookFragment";

  public interface OnComicBookListener {

    void onComicBookCancel();

    void onComicBookSaved(ComicDetails comicDetails);
  }

  private OnComicBookListener mCallback;

  private CollectorViewModel mCollectorViewModel;

  private ComicDetails mComicDetails;

  public static ComicBookFragment newInstance(ComicDetails comicDetails) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    ComicBookFragment fragment = new ComicBookFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicDetails);
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
      mComicDetails = (ComicDetails) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
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

    mCollectorViewModel.getComic(mComicDetails.PublisherCode, mComicDetails.SeriesCode, mComicDetails.IssueCode).observe(
      getViewLifecycleOwner(),
      comicDetails -> {

        if (comicDetails != null) {
          mComicDetails = comicDetails;
        }

        nameEdit.setText(mComicDetails.Title);
        publishEdit.setText(mComicDetails.Published);
        seriesText.setText(mComicDetails.SeriesTitle);
        publisherText.setText(mComicDetails.Publisher);
        volumeText.setText(String.valueOf(mComicDetails.Volume));
        issueText.setText(String.valueOf(mComicDetails.getIssueNumber()));
        productCodeText.setText(mComicDetails.getProductCode());

        ownedButton.setChecked(mComicDetails.IsOwned);
        readButton.setChecked(mComicDetails.HasRead);
    });

    ownedButton.setChecked(false);
    readButton.setChecked(false);

    cancelButton.setOnClickListener(v -> mCallback.onComicBookCancel());
    saveButton.setOnClickListener(v -> {

      Log.d(TAG, "++onClick()");
      ComicBookEntity comicBookEntity = mComicDetails.toEntity();
      comicBookEntity.Title = nameEdit.getText().toString();
      comicBookEntity.IsOwned = ownedButton.isChecked();
      comicBookEntity.HasRead = readButton.isChecked();
      comicBookEntity.PublishedDate = publishEdit.getText().toString();

      mCollectorViewModel.insertComicBook(comicBookEntity);
      mCallback.onComicBookSaved(mComicDetails);
    });
  }
}

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.PublisherViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class PublisherFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "PublisherFragment";

  public interface OnPublisherListener {

    void onPublisherFound(ComicBookEntity comicBookEntity);

    void onPublisherCancel();
  }

  private OnPublisherListener mCallback;

  private Button mContinueButton;
  private EditText mPublisherId;
  private EditText mPublisherName;

  private PublisherViewModel mPublisherViewModel;

  private ComicBookEntity mComicBookEntity;

  public static PublisherFragment newInstance(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    PublisherFragment fragment = new PublisherFragment();
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
      mCallback = (OnPublisherListener) context;
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
      mComicBookEntity = (ComicBookEntity)arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mPublisherViewModel = ViewModelProviders.of(this).get(PublisherViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_publisher, container, false);
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
    Button cancelButton = view.findViewById(R.id.publisher_button_cancel);
    mContinueButton = view.findViewById(R.id.publisher_button_continue);
    mPublisherId = view.findViewById(R.id.publisher_edit_id);
    mPublisherName = view.findViewById(R.id.publisher_edit_name);

    mPublisherViewModel.getAll().observe(this, publisherEntityList -> {

      String publisherId = mComicBookEntity.getPublisherId();
      boolean foundPublisher = false;
      for(PublisherEntity publisherEntity : publisherEntityList) {
        if (publisherEntity.Id.equals(publisherId)) {
          foundPublisher = true;
          break;
        }
      }

      if (foundPublisher) {
        mCallback.onPublisherFound(mComicBookEntity);
      } else {
        updateUI();
      }
    });

    cancelButton.setOnClickListener(v -> mCallback.onPublisherCancel());
    mPublisherName.addTextChangedListener(new TextWatcher() {

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

    mContinueButton.setOnClickListener(v -> {

      Log.d(TAG, "++onClick()");
      PublisherEntity publisherEntity = new PublisherEntity();
      publisherEntity.Id = mComicBookEntity.getPublisherId();
      publisherEntity.Name = mPublisherName.getText().toString();
      mPublisherViewModel.insert(publisherEntity);

      FirebaseFirestore.getInstance().document(PublisherEntity.ROOT).set(publisherEntity, SetOptions.merge()).addOnCompleteListener(task -> {

        if (!task.isSuccessful()) { // not fatal but we need to know this information for review
          Crashlytics.logException(
            new ComicCollectorException(
              String.format(
                Locale.US,
                "Could not write pending publisher: %s",
                publisherEntity.toString())));
        }

        mCallback.onPublisherFound(mComicBookEntity);
      });
    });
  }

  private void updateUI() {

    Log.d(TAG, "++updateUI()");
    mPublisherId.setText(mComicBookEntity.getPublisherId());
  }

  private void validateAll() {

    if (mPublisherName.getText().toString().length() > 0) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

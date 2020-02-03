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
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.SeriesViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class SeriesFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesFragment";

  public interface OnSeriesListener {

    void onSeriesFound(ComicBookEntity comicBookEntity);

    void onSeriesCancel();
  }

  private OnSeriesListener mCallback;

  private Button mContinueButton;
  private EditText mIdEdit;
  private EditText mNameEdit;
  private EditText mVolumeEdit;

  private SeriesViewModel mSeriesViewModel;

  private ComicBookEntity mComicBookEntity;

  public static SeriesFragment newInstance(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    SeriesFragment fragment = new SeriesFragment();
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
      mCallback = (OnSeriesListener) context;
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

    mSeriesViewModel = ViewModelProviders.of(this).get(SeriesViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_series, container, false);
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
    Button cancelButton = view.findViewById(R.id.series_button_cancel);
    mContinueButton = view.findViewById(R.id.series_button_continue);
    mIdEdit = view.findViewById(R.id.series_edit_id);
    mNameEdit = view.findViewById(R.id.series_edit_name);
    mVolumeEdit = view.findViewById(R.id.series_edit_volume);

    mSeriesViewModel.getAll().observe(this, seriesEntityList -> {

      boolean foundSeries = false;
      for (SeriesEntity seriesEntity : seriesEntityList) {
        if (seriesEntity.Id.equals(mComicBookEntity.ProductCode)) {
          foundSeries = true;
          break;
        }
      }

      if (foundSeries) {
        mCallback.onSeriesFound(mComicBookEntity);
      } else {
        updateUI();
      }
    });

    cancelButton.setOnClickListener(v -> mCallback.onSeriesCancel());
    mNameEdit.addTextChangedListener(new TextWatcher() {

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
      SeriesEntity seriesEntity = new SeriesEntity();
      seriesEntity.Id = mComicBookEntity.ProductCode;
      seriesEntity.SeriesId = mIdEdit.getText().toString();
      seriesEntity.PublisherId = mComicBookEntity.getPublisherId();
      seriesEntity.Name = mNameEdit.getText().toString();
      String volume = mVolumeEdit.getText().toString();
      if (!volume.isEmpty()) {
        seriesEntity.Volume = Integer.parseInt(volume);
      }

      mSeriesViewModel.insert(seriesEntity);
      seriesEntity.NeedsReview = true;
      FirebaseFirestore.getInstance().document(SeriesEntity.ROOT).set(seriesEntity, SetOptions.merge())
        .addOnCompleteListener(task -> {

          if (!task.isSuccessful()) { // not fatal but we need to know this information for review
            Crashlytics.logException(
              new ComicCollectorException(
                String.format(
                  Locale.US,
                  "Could not write pending series: %s",
                  seriesEntity.toString())));
          }

          mCallback.onSeriesFound(mComicBookEntity);
        });
    });
  }


  private void updateUI() {

    Log.d(TAG, "++updateUI()");
    mIdEdit.setText(mComicBookEntity.getSeriesId());
  }

  private void validateAll() {

    if (mNameEdit.getText().toString().length() > 0) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

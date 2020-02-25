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
import androidx.lifecycle.ViewModelProvider;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Calendar;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class SeriesFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesFragment";

  public interface OnSeriesListener {

    void onSeriesCancel();

    void onSeriesUpdated(ComicDetails comicDetails);
  }

  private OnSeriesListener mCallback;

  private Button mContinueButton;
  private EditText mProductCodeEdit;
  private EditText mPublisherCodeEdit;
  private EditText mPublisherNameEdit;
  private EditText mSeriesNameEdit;
  private EditText mVolumeEdit;

  private CollectorViewModel mCollectorViewModel;

  private SeriesDetails mSeriesDetails;

  public static SeriesFragment newInstance(SeriesDetails seriesDetails) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    SeriesFragment fragment = new SeriesFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_SERIES, seriesDetails);
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
      if (arguments.containsKey(BaseActivity.ARG_SERIES)) {
        mSeriesDetails = (SeriesDetails) arguments.getSerializable(BaseActivity.ARG_SERIES);
      } else {
        mSeriesDetails = null;
      }
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
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
    mProductCodeEdit = view.findViewById(R.id.series_edit_product_code);
    mPublisherCodeEdit = view.findViewById(R.id.series_edit_publisher_code);
    mPublisherNameEdit = view.findViewById(R.id.series_edit_publisher_name);
    mSeriesNameEdit = view.findViewById(R.id.series_edit_name);
    mVolumeEdit = view.findViewById(R.id.series_edit_volume);

    mCollectorViewModel.getPublisherById(mSeriesDetails.PublisherId).observe(getViewLifecycleOwner(), publisherEntity -> {

      if (publisherEntity == null) {
        PublisherEntity entity = mSeriesDetails.toPublisherEntity();
        entity.SubmittedBy = ""; // TODO: carry over user account
        entity.SubmissionDate = Calendar.getInstance().getTimeInMillis();
        entity.NeedsReview = true;
        mCollectorViewModel.insertPublisher(entity);
        FirebaseFirestore.getInstance().collection(PublisherEntity.ROOT).document(entity.Id).set(entity, SetOptions.merge())
          .addOnCompleteListener(task -> {

            if (!task.isSuccessful()) { // not fatal but we need to know this information for review
              Crashlytics.logException(
                new ComicCollectorException(
                  String.format(
                    Locale.US,
                    "Could not write pending publisher: %s",
                    entity.toString())));
              // TODO: failed to add publisher, where should we go?
            } else {
              updateUI();
            }
          });
      } else {
        updateUI();
      }
    });

    cancelButton.setOnClickListener(v -> mCallback.onSeriesCancel());
    mProductCodeEdit.addTextChangedListener(new TextWatcher() {

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

    mSeriesNameEdit.addTextChangedListener(new TextWatcher() {

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
      if (mPublisherNameEdit.isEnabled()) {
        PublisherEntity entity = new PublisherEntity();
        entity.Name = mPublisherNameEdit.getText().toString();
        entity.PublisherCode = mPublisherCodeEdit.getText().toString();
        mCollectorViewModel.insertPublisher(entity);
        entity.SubmissionDate = Calendar.getInstance().getTimeInMillis();
        entity.NeedsReview = true;
        FirebaseFirestore.getInstance().collection(PublisherEntity.ROOT).document(entity.Id).set(entity, SetOptions.merge())
          .addOnCompleteListener(task -> {

            if (!task.isSuccessful()) { // not fatal but we need to know this information for review
              Crashlytics.logException(
                new ComicCollectorException(
                  String.format(
                    Locale.US,
                    "Could not write pending publisher: %s",
                    entity.toString())));
            }

            mSeriesDetails.Publisher = entity.Name;
            mSeriesDetails.PublisherCode = entity.PublisherCode;
            mSeriesDetails.PublisherId = entity.Id;
            insertSeriesEntity();
          });
      } else {
        insertSeriesEntity();
      }
    });
  }

  private void insertSeriesEntity() {

    Log.d(TAG, "++insertSeriesEntity()");
    SeriesEntity entity = new SeriesEntity();
    entity.PublisherId = mSeriesDetails.PublisherId;
    entity.SeriesCode = mProductCodeEdit.getText().toString();
    entity.Name = mSeriesNameEdit.getText().toString();
    String volume = mVolumeEdit.getText().toString();
    if (!volume.isEmpty()) {
      entity.Volume = Integer.parseInt(volume);
    }

    mCollectorViewModel.insertSeries(entity);
    entity.NeedsReview = true;
    entity.SubmissionDate = Calendar.getInstance().getTimeInMillis();
    FirebaseFirestore.getInstance().collection(SeriesEntity.ROOT).document(entity.Id).set(entity, SetOptions.merge())
      .addOnCompleteListener(task -> {

        if (!task.isSuccessful()) { // not fatal but we need to know this information for review
          Crashlytics.logException(
            new ComicCollectorException(
              String.format(
                Locale.US,
                "Could not write pending series: %s",
                entity.toString())));
        }

        ComicDetails comicDetails = new ComicDetails();
        comicDetails.PublisherId = mSeriesDetails.PublisherId;
        comicDetails.Publisher = mSeriesDetails.Publisher;
        comicDetails.PublisherCode = mSeriesDetails.PublisherCode;
        comicDetails.SeriesId = entity.Id;
        comicDetails.SeriesCode = entity.SeriesCode;
        comicDetails.SeriesTitle = entity.Name;
        comicDetails.Volume = entity.Volume;
        mCallback.onSeriesUpdated(comicDetails);
      });
  }

  private void updateUI() {

    if (mSeriesDetails != null) {
      if (mSeriesDetails.PublisherCode.equals(BaseActivity.DEFAULT_PUBLISHER_CODE) ||
        mSeriesDetails.PublisherCode.length() != BaseActivity.DEFAULT_PUBLISHER_CODE.length()) {
        mPublisherCodeEdit.setEnabled(true);
      } else {
        mPublisherCodeEdit.setEnabled(false);
        mPublisherCodeEdit.setText(mSeriesDetails.PublisherCode);
      }

      if (mSeriesDetails.Publisher.isEmpty()) {
        mPublisherNameEdit.setEnabled(true);
      } else {
        mPublisherNameEdit.setEnabled(false);
        mPublisherNameEdit.setText(mSeriesDetails.Publisher);
      }

      if (!mSeriesDetails.SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE)) {
        mProductCodeEdit.setEnabled(false);
        mProductCodeEdit.setText(mSeriesDetails.SeriesCode);
      } else {
        mProductCodeEdit.setEnabled(true);
      }

      if (!mSeriesDetails.SeriesTitle.isEmpty()) {
        mSeriesNameEdit.setText(mSeriesDetails.SeriesTitle);
      }

      if (mSeriesDetails.Volume > 0) {
        mVolumeEdit.setText(String.valueOf(mSeriesDetails.Volume));
      }
    }
  }

  private void validateAll() {

    String productCode = mProductCodeEdit.getText().toString();
    String productTitle = mSeriesNameEdit.getText().toString();
    if (!productCode.equals(BaseActivity.DEFAULT_SERIES_CODE) &&
      productCode.length() == BaseActivity.DEFAULT_SERIES_CODE.length() &&
      !productTitle.isEmpty()) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

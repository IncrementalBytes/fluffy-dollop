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
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class SeriesFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesFragment";

  public interface OnSeriesListener {

    void onSeriesCancel();

    void onSeriesSearched(SeriesEntity seriesEntity);
  }

  private OnSeriesListener mCallback;

  private Button mContinueButton;
  private EditText mProductCodeEdit;
  private EditText mPublisherNameEdit;
  private EditText mSeriesNameEdit;
  private EditText mVolumeEdit;
  private ProgressBar mWaitProgress;
  private TextView mWaitText;

  private CollectorViewModel mCollectorViewModel;

  private String mProductCode;
  private SeriesEntity mSeriesEntity;

  public static SeriesFragment newInstance(String productCode) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    SeriesFragment fragment = new SeriesFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_PRODUCT_CODE, productCode);
    fragment.setArguments(arguments);
    return fragment;
  }

  public static SeriesFragment newInstance(SeriesEntity seriesEntity) {

    Log.d(TAG, "++newInstance(ComicBookEntry)");
    SeriesFragment fragment = new SeriesFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_SERIES, seriesEntity);
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
        mSeriesEntity = (SeriesEntity) arguments.getSerializable(BaseActivity.ARG_SERIES);
      } else {
        mSeriesEntity = null;
      }

      if (arguments.containsKey(BaseActivity.ARG_PRODUCT_CODE)) {
        mProductCode = arguments.getString(BaseActivity.ARG_PRODUCT_CODE);
      } else {
        mProductCode = null;
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
    mProductCodeEdit = view.findViewById(R.id.series_edit_product_code_value);
    mPublisherNameEdit = view.findViewById(R.id.series_edit_publisher_name_value);
    mSeriesNameEdit = view.findViewById(R.id.series_edit_name_value);
    mVolumeEdit = view.findViewById(R.id.series_edit_volume);
    mWaitProgress = view.findViewById(R.id.series_progress_wait);
    mWaitText = view.findViewById(R.id.series_text_wait);

    // 2 Acceptable Scenarios:
    //   1) ProductCode - search for series
    //   2) SeriesEntity - edit series

    if (mSeriesEntity == null) {
      mContinueButton.setVisibility(View.INVISIBLE);
      mProductCodeEdit.setVisibility(View.INVISIBLE);
      mPublisherNameEdit.setVisibility(View.INVISIBLE);
      mSeriesNameEdit.setVisibility(View.INVISIBLE);
      mVolumeEdit.setVisibility(View.INVISIBLE);
      mWaitProgress.setIndeterminate(true);
      mCollectorViewModel.findSeries(mProductCode).observe(getViewLifecycleOwner(), seriesEntity -> {

        if (seriesEntity != null) {
          mCallback.onSeriesSearched(seriesEntity);
        } else { // gather more information about the series from the user
          updateUI();
        }
      });
    } else { // gather more information about the series from the user
      updateUI();
    }

    cancelButton.setOnClickListener(v -> mCallback.onSeriesCancel());
    mPublisherNameEdit.addTextChangedListener(new TextWatcher() {
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
      SeriesEntity seriesEntity = new SeriesEntity();
      seriesEntity.Id = mProductCode;
      seriesEntity.Publisher = mPublisherNameEdit.getText().toString();
      seriesEntity.Name = mSeriesNameEdit.getText().toString();
      String volume = mVolumeEdit.getText().toString();
      if (!volume.isEmpty()) {
        seriesEntity.Volume = Integer.parseInt(volume);
      }

      mCollectorViewModel.insertSeries(seriesEntity);

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

          mCallback.onSeriesSearched(seriesEntity);
        });
    });
  }


  private void updateUI() {

    Log.d(TAG, "++updateUI()");
    mContinueButton.setVisibility(View.VISIBLE);
    mProductCodeEdit.setVisibility(View.VISIBLE);
    mPublisherNameEdit.setVisibility(View.VISIBLE);
    mSeriesNameEdit.setVisibility(View.VISIBLE);
    mVolumeEdit.setVisibility(View.VISIBLE);
    mWaitProgress.setVisibility(View.INVISIBLE);
    mWaitText.setVisibility(View.INVISIBLE);

    mProductCodeEdit.setText(mProductCode);
  }

  private void validateAll() {

    if ((mSeriesNameEdit.getText().toString().length() > 0) &&
      (mPublisherNameEdit.getText().toString().length() > 0)) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

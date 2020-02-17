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
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchBookFound(ComicDetails comicDetails);

    void onManualSearchInputComplete(ComicDetails comicDetails);

    void onManualSearchRetry();
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private CollectorViewModel mCollectorViewModel;
  private SeriesEntity mSeriesEntity;

  public static ManualSearchFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new ManualSearchFragment();
  }

  public static ManualSearchFragment newInstance(SeriesEntity seriesEntity) {

    Log.d(TAG, "++newInstance(SeriesEntity)");
    ManualSearchFragment fragment = new ManualSearchFragment();
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
      mCallback = (OnManualSearchListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    mSeriesEntity = null;
    Bundle arguments = getArguments();
    if (arguments != null) {
      if (arguments.containsKey(BaseActivity.ARG_SERIES)) {
        mSeriesEntity = (SeriesEntity) arguments.getSerializable(BaseActivity.ARG_SERIES);
      }
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_manual_search, container, false);
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

    Button retryButton = view.findViewById(R.id.manual_search_button_retry);
    retryButton.setOnClickListener(v -> mCallback.onManualSearchRetry());
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      String productCode = mProductCodeEdit.getText().toString();
      String issueCode = mIssueCodeEdit.getText().toString();
      mCollectorViewModel.findComic(productCode, issueCode).observe(getViewLifecycleOwner(), comicDetails -> {

        if (comicDetails == null) { // not found, new comic entry
          comicDetails = new ComicDetails();
          comicDetails.setIssueCode(issueCode);
          comicDetails.ProductCode = productCode;
          comicDetails.PublisherName = mSeriesEntity.Publisher;
          comicDetails.SeriesTitle = mSeriesEntity.Name;
          comicDetails.Volume = mSeriesEntity.Volume;
          mCallback.onManualSearchInputComplete(comicDetails);
        } else {
          mCallback.onManualSearchBookFound(comicDetails);
        }
      });
    });

    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);
    if (mSeriesEntity != null) {
      mProductCodeEdit.setText(mSeriesEntity.Id);
    }

    // setup text change watchers
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

    mIssueCodeEdit.addTextChangedListener(new TextWatcher() {

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
  }

  /*
    Private Method(s)
  */
  private void validateAll() {

    if (mProductCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() &&
      !mProductCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
      mIssueCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_ISSUE_CODE.length() &&
      !mIssueCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

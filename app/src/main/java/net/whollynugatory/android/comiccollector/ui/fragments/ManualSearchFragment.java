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
import androidx.lifecycle.ViewModelProviders;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.db.viewmodel.ComicBookViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchBookFound(ComicBookEntity comicBookEntity);

    void onManualSearchInputComplete(ComicBookEntity comicBookEntity);

    void onManualSearchRetry();
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private ComicBookViewModel mComicBookViewModel;

  private String mProductCode;

  public static ManualSearchFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new ManualSearchFragment();
  }

  public static ManualSearchFragment newInstance(String productCode) {

    Log.d(TAG, "++newInstance(String)");
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle arguments = new Bundle();
    arguments.putString(BaseActivity.ARG_PRODUCT_CODE, productCode);
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
    Bundle arguments = getArguments();
    if (arguments != null) {
      mProductCode = arguments.getString(BaseActivity.ARG_PRODUCT_CODE);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mComicBookViewModel = ViewModelProviders.of(this).get(ComicBookViewModel.class);
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

    // 2 Possible Scenarios:
    //   1) Product Code (Publisher & Series) is known, we need the IssueCode
    //   2) We need Product Code (Publisher & Series), & IssueCode

    Button retryButton = view.findViewById(R.id.manual_search_button_retry);
    retryButton.setOnClickListener(v -> mCallback.onManualSearchRetry());
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      String productCode = mProductCodeEdit.getText().toString();
      String issueCode = mIssueCodeEdit.getText().toString();
      mComicBookViewModel.find(productCode, issueCode).observe(this, comicBookEntity -> {

        if (comicBookEntity == null) { // not found, new comic entry
          comicBookEntity = new ComicBookEntity(productCode, issueCode);
          mCallback.onManualSearchInputComplete(comicBookEntity);
        } else {
          mCallback.onManualSearchBookFound(comicBookEntity);
        }
      });
    });

    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);

    if (mProductCode != null && !mProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
      mProductCodeEdit.setText(mProductCode);
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

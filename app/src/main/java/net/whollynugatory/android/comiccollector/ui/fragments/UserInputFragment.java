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
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

public class UserInputFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "UserInputFragment";

  public interface OnUserInputListener {

    void onUserInputBookFound(ComicDetails comicDetails);

    void onUserInputComplete(ComicDetails comicDetails);

    void onUserInputRetry();
  }

  private OnUserInputListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private CollectorViewModel mCollectorViewModel;
  private ComicDetails mComicDetails;

  public static UserInputFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new UserInputFragment();
  }

  public static UserInputFragment newInstance(ComicDetails comicDetails) {

    Log.d(TAG, "++newInstance(SeriesEntity)");
    UserInputFragment fragment = new UserInputFragment();
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
      mCallback = (OnUserInputListener) context;
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
    mComicDetails = null;
    if (arguments != null) {
      if (arguments.containsKey(BaseActivity.ARG_COMIC_BOOK)) {
        mComicDetails = (ComicDetails) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
      }
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_user_input, container, false);
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

    Button retryButton = view.findViewById(R.id.user_input_button_retry);
    retryButton.setOnClickListener(v -> mCallback.onUserInputRetry());
    mContinueButton = view.findViewById(R.id.user_input_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      String publisherId = PublisherEntity.getPublisherCode(mProductCodeEdit.getText().toString());
      String seriesId = SeriesEntity.getSeriesCode(mProductCodeEdit.getText().toString());
      String issueCode = mIssueCodeEdit.getText().toString();
      mCollectorViewModel.getComic(publisherId, seriesId, issueCode).observe(getViewLifecycleOwner(), comicDetails -> {

        if (comicDetails == null) { // not found, new comic entry
          mComicDetails.setIssueCode(issueCode);
          mCallback.onUserInputComplete(mComicDetails);
        } else {
          mCallback.onUserInputBookFound(mComicDetails);
        }
      });
    });

    mProductCodeEdit = view.findViewById(R.id.user_input_edit_product);
    mIssueCodeEdit = view.findViewById(R.id.user_input_edit_issue);
    if (mComicDetails != null) {
      mProductCodeEdit.setText(mComicDetails.getProductCode());
      mProductCodeEdit.setEnabled(false);
    } else { // setup text change watcher
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
    }

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

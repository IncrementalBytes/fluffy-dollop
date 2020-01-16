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
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.LogUtils;
import net.whollynugatory.android.comiccollector.db.entity.ComicBook;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(ComicBook comicBook);

    void onManualSearchRetry();
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private ComicBook mComicBook;

  public static ManualSearchFragment newInstance(ComicBook comicBook) {

    LogUtils.debug(TAG, "++newInstance(%s)", comicBook.toString());
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicBook);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
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

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicBook = (ComicBook) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_manual_search, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    LogUtils.debug(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    LogUtils.debug(TAG, "++onViewCreated(View, Bundle)");

    // 2 Scenarios:
    //   1) Product Code (Publisher & Series) is known, we need the IssueCode
    //   2) We need Product Code (Publisher & Series), & IssueCode

    Button retryButton = view.findViewById(R.id.manual_search_button_retry);
    retryButton.setOnClickListener(v -> mCallback.onManualSearchRetry());
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {
      mComicBook = new ComicBook();
      mComicBook.parseProductCode(
        String.format(
          Locale.US,
          "%s-%s",
          mProductCodeEdit.getText().toString(),
          mIssueCodeEdit.getText().toString()));
      mCallback.onManualSearchActionComplete(mComicBook);
    });

    TextView productCodeExampleText = view.findViewById(R.id.manual_search_text_product_example);
    ImageView productCodeImage = view.findViewById(R.id.manual_search_image_product);
    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
    TextView issueCodeText = view.findViewById(R.id.manual_search_text_issue);
    TextView issueCodeExampleText = view.findViewById(R.id.manual_search_text_issue_example);
    ImageView issueCodeImage = view.findViewById(R.id.manual_search_image_issue);
    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);
    TextView messageText = view.findViewById(R.id.manual_search_text_no_barcode);

    if (mComicBook == null) {
      // we need both ProductCode and IssueCode
      messageText.setVisibility(View.VISIBLE);
      productCodeExampleText.setVisibility(View.VISIBLE);
      productCodeImage.setVisibility(View.VISIBLE);
      issueCodeExampleText.setVisibility(View.VISIBLE);
      issueCodeImage.setVisibility(View.VISIBLE);
    } else {
      if (mComicBook.ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) ||
        mComicBook.ProductCode.length() != BaseActivity.DEFAULT_PRODUCT_CODE.length()) { // we need ProductCode
        productCodeExampleText.setVisibility(View.VISIBLE);
        productCodeImage.setVisibility(View.VISIBLE);
      } else {
        messageText.setVisibility(View.GONE);
        productCodeExampleText.setVisibility(View.GONE);
        productCodeImage.setVisibility(View.GONE);
        mProductCodeEdit.setText(mComicBook.ProductCode);
        mProductCodeEdit.setEnabled(false);
      }

      if (mComicBook.IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) ||
        mComicBook.IssueCode.length() != BaseActivity.DEFAULT_ISSUE_CODE.length()) {
        issueCodeText.setVisibility(View.VISIBLE);
        issueCodeImage.setVisibility(View.VISIBLE);
      } else {
        messageText.setVisibility(View.GONE);
        issueCodeExampleText.setVisibility(View.GONE);
        issueCodeImage.setVisibility(View.GONE);
        mIssueCodeEdit.setText(mComicBook.IssueCode);
        mIssueCodeEdit.setEnabled(false);
      }
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

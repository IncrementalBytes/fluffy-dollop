package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(ComicBook comicBook);

    void onManualSearchCancel();
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
    args.putParcelable(BaseActivity.ARG_COMIC_BOOK, comicBook);
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

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicBook = arguments.getParcelable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_manual_search, container, false);

    // 2 Scenarios:
    //   1) ComicBook has an ID (Publisher & Series), we just need the issue
    //   2) ComicBook is empty/null, we need Publisher, Series, & Issue

    Button cancelButton = view.findViewById(R.id.manual_search_button_cancel);
    cancelButton.setOnClickListener(v -> mCallback.onManualSearchCancel());
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      mComicBook.parseProductCode(mProductCodeEdit.getText().toString().trim());
      mComicBook.parseIssueCode(mIssueCodeEdit.getText().toString().trim());
      if (mComicBook == null || !mComicBook.isValid()) {
        mCallback.onManualSearchCancel();
      } else {
        mCallback.onManualSearchActionComplete(mComicBook);
      }
    });

    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);
    TextView messageText = view.findViewById(R.id.manual_search_text_no_barcode);
    if (mComicBook == null || !isValid()) {
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
    } else {
      messageText.setVisibility(View.INVISIBLE);
      mProductCodeEdit.setText(mComicBook.getProductId());
      mProductCodeEdit.setEnabled(false);
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

    return view;
  }

  /*
    Private Method(s)
  */
  private boolean isValid() {

    LogUtils.debug(TAG, "++isValid()");
    if (mComicBook.PublisherId == null ||
      mComicBook.PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
      mComicBook.PublisherId.length() != BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length()) {
      LogUtils.debug(TAG, "Publisher data is unexpected: %s", mComicBook.PublisherId);
      return false;
    }

    if (mComicBook.SeriesId == null ||
      mComicBook.SeriesId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID) ||
      mComicBook.SeriesId.length() != BaseActivity.DEFAULT_COMIC_SERIES_ID.length()) {
      LogUtils.debug(TAG, "Series data is unexpected: %s", mComicBook.SeriesId);
      return false;
    }
    return true;
  }

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

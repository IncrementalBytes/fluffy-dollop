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
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BASE_TAG + ManualSearchFragment.class.getSimpleName();

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(String seriesCode, String issueCode);
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueEdit;
  private EditText mSeriesEdit;

  private String mSeriesCode;

  public static ManualSearchFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new ManualSearchFragment();
  }

  public static ManualSearchFragment newInstance(String seriesCode) {

    LogUtils.debug(TAG, "++newInstance(%s)", seriesCode);
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_COMIC_SERIES_CODE, seriesCode);
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
      mSeriesCode = arguments.getString(BaseActivity.ARG_COMIC_SERIES_CODE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_manual_search, container, false);
    Button cancelButton = view.findViewById(R.id.manual_search_button_cancel);
    cancelButton.setOnClickListener(v -> mCallback.onManualSearchActionComplete("", ""));

    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v ->
        mCallback.onManualSearchActionComplete(mSeriesEdit.getText().toString(), mIssueEdit.getText().toString()));

    mSeriesEdit = view.findViewById(R.id.manual_search_edit_series);
    if (mSeriesCode == null || mSeriesCode.length() < 1) {
      mSeriesEdit.addTextChangedListener(new TextWatcher() {
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
      mSeriesEdit.setText(mSeriesCode);
      mSeriesEdit.setEnabled(false);
    }

    mIssueEdit = view.findViewById(R.id.manual_search_edit_issue);
    mIssueEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        validateAll();
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });

    return view;
  }

  private void validateAll() {

    if (mSeriesEdit.getText().toString().length() == BaseActivity.DEFAULT_SERIES_CODE.length() &&
        !mSeriesEdit.getText().toString().equals(BaseActivity.DEFAULT_SERIES_CODE) &&
        mIssueEdit.getText().toString().length() == BaseActivity.DEFAULT_ISSUE_CODE.length() &&
        !mIssueEdit.getText().toString().equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

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
package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;

public class ComicSeriesFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicSeriesFragment";

  public interface OnComicSeriesListener {

    void onComicSeriesActionComplete(ComicSeriesDetails comicSeries);
  }

  private OnComicSeriesListener mCallback;

  private EditText mSeriesIdEdit;
  private EditText mPublisherNameEdit;
  private Button mContinueButton;
  private EditText mSeriesNameEdit;
  private EditText mSeriesVolumeEdit;
  private Button mCancelButton;

  private ComicSeriesDetails mComicSeries;

  public static ComicSeriesFragment newInstance(ComicSeriesDetails comicSeries) {

    LogUtils.debug(TAG, "++newInstance(%s)", comicSeries.toString());
    ComicSeriesFragment fragment = new ComicSeriesFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_SERIES, comicSeries);
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
      mCallback = (OnComicSeriesListener) context;
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
      mComicSeries = (ComicSeriesDetails)arguments.getSerializable(BaseActivity.ARG_COMIC_SERIES);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_comic_series, container, false);
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
    mSeriesIdEdit = view.findViewById(R.id.comic_series_edit_code);
    mPublisherNameEdit = view.findViewById(R.id.comic_series_edit_publisher);
    mSeriesNameEdit = view.findViewById(R.id.comic_series_edit_name);
    mSeriesVolumeEdit = view.findViewById(R.id.comic_series_edit_volume);
    mCancelButton = view.findViewById(R.id.comic_series_button_cancel);
    mContinueButton = view.findViewById(R.id.comic_series_button_continue);
    updateUI();
  }

  /*
    Private Method(s)
   */
  private void updateUI() {

    LogUtils.debug(TAG, "++updateUI()");
    mSeriesIdEdit.setText(mComicSeries.Id);
    mSeriesIdEdit.setEnabled(false);
    mPublisherNameEdit.setText(mComicSeries.PublisherName);
    mPublisherNameEdit.setEnabled(false);

    mSeriesNameEdit.setText(mComicSeries.Title);
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

    mSeriesVolumeEdit.addTextChangedListener(new TextWatcher() {
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

    mCancelButton.setOnClickListener(v -> mCallback.onComicSeriesActionComplete(null));

    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      mComicSeries.Title = mSeriesNameEdit.getText().toString().trim();
      if (!mSeriesVolumeEdit.getText().toString().isEmpty()) {
        mComicSeries.Volume = Integer.parseInt(mSeriesVolumeEdit.getText().toString());
      }

      mCallback.onComicSeriesActionComplete(mComicSeries);
    });
  }

  private void validateAll() {

    if (!mSeriesNameEdit.getText().toString().isEmpty()) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

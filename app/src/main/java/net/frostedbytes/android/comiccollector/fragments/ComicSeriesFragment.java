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
import androidx.lifecycle.ViewModelProviders;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;
import net.frostedbytes.android.comiccollector.viewmodel.CollectorViewModel;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicSeriesFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicSeriesFragment";

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

  private String mProductCode;

  public static ComicSeriesFragment newInstance(String productCode) {

    LogUtils.debug(TAG, "++newInstance()");
    ComicSeriesFragment fragment = new ComicSeriesFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_PRODUCT_CODE, productCode);
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
      mProductCode = arguments.getString(BaseActivity.ARG_PRODUCT_CODE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    CollectorViewModel mCollectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
    mCollectorViewModel.getComicSeriesByProductCode(mProductCode).observe(this, this::updateUI);

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
  }

  /*
    Private Method(s)
   */
  private void updateUI(ComicSeriesDetails comicSeries) {

    LogUtils.debug(TAG, "++updateUI(%s)", comicSeries.toString());
    mSeriesIdEdit.setText(comicSeries.Id);
    mSeriesIdEdit.setEnabled(false);
    mPublisherNameEdit.setText(getString(R.string.not_available));
    mPublisherNameEdit.setEnabled(false);

    mSeriesNameEdit.setText(comicSeries.Title);
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

      comicSeries.Title = mSeriesNameEdit.getText().toString().trim();
      if (!mSeriesVolumeEdit.getText().toString().isEmpty()) {
        comicSeries.Volume = Integer.parseInt(mSeriesVolumeEdit.getText().toString());
      }

      mCallback.onComicSeriesActionComplete(comicSeries);
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

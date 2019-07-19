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
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicSeriesFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicSeriesFragment";

  public interface OnComicSeriesListener {

    void onComicSeriesActionComplete(ComicSeries comicSeries);
  }

  private OnComicSeriesListener mCallback;

  private Button mContinueButton;
  private EditText mSeriesNameEdit;
  private EditText mSeriesVolumeEdit;

  private ComicPublisher mComicPublisher;
  private ComicSeries mComicSeries;

  /**
   * Creates a new instance of the ComicSeriesFragment.
   * @param comicSeries Partial or fully initialized ComicSeries object.
   * @param comicPublisher ComicPublisher object with additional information to display.
   * @return Newly initialized ComicSeriesFragment object.
   */
  public static ComicSeriesFragment newInstance(ComicSeries comicSeries, ComicPublisher comicPublisher) {

    LogUtils.debug(TAG, "++newInstance()");
    ComicSeriesFragment fragment = new ComicSeriesFragment();
    Bundle args = new Bundle();
    args.putParcelable(BaseActivity.ARG_COMIC_PUBLISHER, comicPublisher);
    args.putParcelable(BaseActivity.ARG_COMIC_SERIES, comicSeries);
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

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicPublisher = arguments.getParcelable(BaseActivity.ARG_COMIC_PUBLISHER);
      mComicSeries = arguments.getParcelable(BaseActivity.ARG_COMIC_SERIES);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_comic_series, container, false);

    EditText seriesIdEdit = view.findViewById(R.id.comic_series_edit_code);
    seriesIdEdit.setText(mComicSeries.Id);
    seriesIdEdit.setEnabled(false);

    EditText mPublisherNameEdit = view.findViewById(R.id.comic_series_edit_publisher);
    mPublisherNameEdit.setText(mComicPublisher.Name);
    mPublisherNameEdit.setEnabled(false);

    mSeriesNameEdit = view.findViewById(R.id.comic_series_edit_name);
    mSeriesNameEdit.setText(mComicSeries.SeriesName);
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

    mSeriesVolumeEdit = view.findViewById(R.id.comic_series_edit_volume);
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

    Button cancelButton = view.findViewById(R.id.comic_series_button_cancel);
    cancelButton.setOnClickListener(v -> mCallback.onComicSeriesActionComplete(null));

    mContinueButton = view.findViewById(R.id.comic_series_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      mComicSeries.SeriesName = mSeriesNameEdit.getText().toString().trim();
      if (!mSeriesVolumeEdit.getText().toString().isEmpty()) {
        mComicSeries.Volume = Integer.parseInt(mSeriesVolumeEdit.getText().toString());
      }

      mCallback.onComicSeriesActionComplete(mComicSeries);
    });

    return view;
  }

  private void validateAll() {

    if (!mSeriesNameEdit.getText().toString().isEmpty()) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

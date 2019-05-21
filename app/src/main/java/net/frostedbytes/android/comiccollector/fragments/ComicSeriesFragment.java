package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils.ByStringValue;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicSeriesFragment extends Fragment {

  private static final String TAG = BASE_TAG + ComicSeriesFragment.class.getSimpleName();

  public interface OnComicSeriesListener {

    void onComicSeriesActionComplete(ComicBook comicBook);
  }

  private OnComicSeriesListener mCallback;

  private Button mContinueButton;
  private EditText mSeriesNameEdit;
  private EditText mPublisherNameEdit;
  private RadioGroup mPublisherRadioGroup;
  private Spinner mPublisherSpinner;
  private EditText mSeriesVolumeEdit;

  private ComicBook mComicBook;
  private ArrayList<String> mKnownPublishers;
  private String mSelectedPublisher;

  public static ComicSeriesFragment newInstance(ComicBook comicBook, ArrayList<String> knownPublishers) {

    LogUtils.debug(TAG, "++newInstance()");
    ComicSeriesFragment fragment = new ComicSeriesFragment();
    Bundle args = new Bundle();
    args.putParcelable(BaseActivity.ARG_COMIC_BOOK, comicBook);
    args.putStringArrayList(BaseActivity.ARG_PUBLISHERS, knownPublishers);
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
      mComicBook = arguments.getParcelable(BaseActivity.ARG_COMIC_BOOK);
      mKnownPublishers = arguments.getStringArrayList(BaseActivity.ARG_PUBLISHERS);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_comic_series, container, false);
    Button cancelButton = view.findViewById(R.id.comic_series_button_cancel);
    cancelButton.setOnClickListener(v -> {

      mComicBook.SeriesName = "";
      mComicBook.Publisher = "";
      mComicBook.Volume = -1;
      mCallback.onComicSeriesActionComplete(mComicBook);
    });

    mContinueButton = view.findViewById(R.id.comic_series_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      mComicBook.SeriesName = mSeriesNameEdit.getText().toString();
      mComicBook.Publisher = mSelectedPublisher;
      mComicBook.Volume = Integer.parseInt(mSeriesVolumeEdit.getText().toString());
      mCallback.onComicSeriesActionComplete(mComicBook);
    });

    EditText seriesCodeEdit = view.findViewById(R.id.comic_series_edit_code);
    seriesCodeEdit.setText(mComicBook.SeriesCode);

    mSeriesNameEdit = view.findViewById(R.id.comic_series_edit_name);
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

    mPublisherSpinner = view.findViewById(R.id.comic_series_spinner_publisher);
    mPublisherNameEdit = view.findViewById(R.id.comic_series_edit_publisher);

    mPublisherRadioGroup = view.findViewById(R.id.comic_series_group_publisher);
    mPublisherRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {

      if (checkedId == R.id.comic_series_button_existing) {
        mPublisherSpinner.setEnabled(true);
        mPublisherNameEdit.setEnabled(false);
      } else {
        mPublisherSpinner.setEnabled(false);
        mPublisherNameEdit.setEnabled(true);
      }
    });

    if (mKnownPublishers == null) {
      mKnownPublishers = new ArrayList<>();
    }

    mKnownPublishers.sort(new ByStringValue());
    mPublisherSpinner = view.findViewById(R.id.comic_series_spinner_publisher);
    if (getActivity() != null) {
      ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mKnownPublishers);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mPublisherSpinner.setAdapter(adapter);

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
    } else {
      mPublisherSpinner.setEnabled(false);
      mPublisherRadioGroup.check(R.id.comic_series_button_new);
    }

    return view;
  }

  private void validateAll() {

    mSelectedPublisher = mPublisherSpinner.getSelectedItem().toString();
    if (mPublisherRadioGroup.getCheckedRadioButtonId() == R.id.comic_series_button_new) {
      mSelectedPublisher = mPublisherNameEdit.getText().toString();
    }

    if (!mSeriesNameEdit.getText().toString().isEmpty() &&
        !mSelectedPublisher.isEmpty() &&
        !mSeriesVolumeEdit.getText().toString().isEmpty()) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}

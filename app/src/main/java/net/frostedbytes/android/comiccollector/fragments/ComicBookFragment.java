package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.DateUtils;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

import java.util.Calendar;
import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicBookFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicBookFragment";

  public interface OnComicBookListener {

    void onComicBookActionComplete(String message);

    void onComicBookAddedToLibrary(ComicBook comicBook);

    void onComicBookInit(boolean isSuccessful);

    void onComicBookRemoved(ComicBook comicBook);
  }

  private OnComicBookListener mCallback;

  private ToggleButton mOwnedToggle;
  private ToggleButton mReadToggle;
  private EditText mTitleEdit;

  private ComicBook mComicBook;
  private ComicPublisher mComicPublisher;
  private ComicSeries mComicSeries;
  private boolean mIsNew;

  public static ComicBookFragment newInstance(ComicBook comicBook, ComicPublisher comicPublisher, ComicSeries comicSeries) {

    return newInstance(comicBook, comicPublisher, comicSeries, false);
  }

  public static ComicBookFragment newInstance(ComicBook comicBook, ComicPublisher comicPublisher, ComicSeries comicSeries, boolean isNew) {

    LogUtils.debug(
      TAG,
      "++newInstance(%s, %s, %s, %s)",
      comicBook.toString(),
      comicPublisher.toString(),
      comicSeries.toString(),
      String.valueOf(isNew));
    ComicBookFragment fragment = new ComicBookFragment();
    Bundle args = new Bundle();
    args.putParcelable(BaseActivity.ARG_COMIC_BOOK, comicBook);
    args.putParcelable(BaseActivity.ARG_COMIC_PUBLISHER, comicPublisher);
    args.putParcelable(BaseActivity.ARG_COMIC_SERIES, comicSeries);
    args.putBoolean(BaseActivity.ARG_NEW_COMIC_BOOK, isNew);
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
      mCallback = (OnComicBookListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicBook = arguments.getParcelable(BaseActivity.ARG_COMIC_BOOK);
      mComicPublisher = arguments.getParcelable(BaseActivity.ARG_COMIC_PUBLISHER);
      mComicSeries = arguments.getParcelable(BaseActivity.ARG_COMIC_SERIES);
      mIsNew = arguments.getBoolean(BaseActivity.ARG_NEW_COMIC_BOOK);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_comic_book, container, false);

    mTitleEdit = view.findViewById(R.id.comic_book_edit_title);
    TextView mSeriesText = view.findViewById(R.id.comic_book_text_series_value);
    TextView mPublisherText = view.findViewById(R.id.comic_book_text_publisher_value);
    EditText mPublishedDateEdit = view.findViewById(R.id.comic_book_edit_published_date);
    TextView mVolumeText = view.findViewById(R.id.comic_book_text_volume_value);
    TextView mIssueText = view.findViewById(R.id.comic_book_text_issue_value);
    TextView mProductCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
    mOwnedToggle = view.findViewById(R.id.comic_book_toggle_owned);
    mReadToggle = view.findViewById(R.id.comic_book_toggle_read);
    Button saveButton = view.findViewById(R.id.comic_book_button_save);
    Button removeButton = view.findViewById(R.id.comic_book_button_remove);

    if (mComicBook == null || !mComicBook.isValid()) {
      mCallback.onComicBookInit(false);
    } else {
      mTitleEdit.setText(mComicBook.Title);
      if (mComicBook.PublishedDate.equals(BaseActivity.DEFAULT_PUBLISHED_DATE) ||
        mComicBook.PublishedDate.length() != BaseActivity.DEFAULT_PUBLISHED_DATE.length()) {
        mPublishedDateEdit.setText(DateUtils.formatDateForDisplay(Calendar.getInstance().getTimeInMillis()));
      } else {
        mPublishedDateEdit.setText(mComicBook.PublishedDate);
      }

      if (mComicPublisher == null) {
        mPublisherText.setText(getString(R.string.placeholder));
      } else {
        mPublisherText.setText(mComicPublisher.Name);
      }

      if (mComicSeries == null) {
        mSeriesText.setText(getString(R.string.placeholder));
        mVolumeText.setText(getString(R.string.placeholder));
      } else {
        mSeriesText.setText(mComicSeries.SeriesName);
        mVolumeText.setText(String.valueOf(mComicSeries.Volume));
      }

      mIssueText.setText(String.valueOf(mComicBook.IssueNumber));
      mProductCodeText.setText(mComicBook.getProductId());
      mOwnedToggle.setChecked(mComicBook.OwnedState);
      mReadToggle.setChecked(mComicBook.ReadState);

      saveButton.setOnClickListener(v -> {

        ComicBook updatedBook = new ComicBook(mComicBook);
        updatedBook.Title = mTitleEdit.getText().toString();
        updatedBook.PublishedDate = mPublishedDateEdit.getText().toString();
        updatedBook.OwnedState = mOwnedToggle.isChecked();
        updatedBook.ReadState = mReadToggle.isChecked();
        mCallback.onComicBookAddedToLibrary(updatedBook);
      });

      if (mIsNew) {
        removeButton.setVisibility(View.INVISIBLE);
      } else {
        removeButton.setOnClickListener(v -> {

          if (getActivity() != null) {
            String message = String.format(Locale.US, getString(R.string.remove_book_message), mComicBook.Title);
            if (mComicBook.Title.isEmpty()) {
              message = "Remove comic book from your library?";
            }

            AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
              .setMessage(message)
              .setPositiveButton(android.R.string.yes, (dialog, which) -> mCallback.onComicBookRemoved(mComicBook))
              .setNegativeButton(android.R.string.no, null)
              .create();
            removeBookDialog.show();
          } else {
            String message = "Unable to get activity; cannot remove book.";
            LogUtils.debug(TAG, message);
            mCallback.onComicBookActionComplete(message);
          }
        });
      }
    }

    mCallback.onComicBookInit(true);

    return view;
  }
}

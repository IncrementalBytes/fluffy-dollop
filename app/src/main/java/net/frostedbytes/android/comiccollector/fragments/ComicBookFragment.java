package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.DateUtils;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

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

    void onComicBookStarted();

    void onComicBookUpdated(ComicBook comicBook);
  }

  private OnComicBookListener mCallback;

  private ToggleButton mOwnedToggle;
  private EditText mPublishedDateEdit;
  private ToggleButton mReadToggle;
  private EditText mTitleEdit;

  private ComicBook mComicBook;
  private ComicPublisher mComicPublisher;
  private ComicSeries mComicSeries;
  private String mUserId;

  public static ComicBookFragment newInstance(String userId, ComicBook comicBook, ComicPublisher comicPublisher, ComicSeries comicSeries) {

    LogUtils.debug(TAG, "++newInstance(%s, %s)", userId, comicBook.toString());
    ComicBookFragment fragment = new ComicBookFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_USER_ID, userId);
    args.putParcelable(BaseActivity.ARG_COMIC_BOOK, comicBook);
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
      mUserId = arguments.getString(BaseActivity.ARG_USER_ID);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_comic_book, container, false);

    if (!mComicBook.isValid()) {
      mCallback.onComicBookInit(false);
    } else {
      mTitleEdit = view.findViewById(R.id.comic_book_edit_title);
      mTitleEdit.setText(mComicBook.Title);
      mPublishedDateEdit = view.findViewById(R.id.comic_book_edit_published_date);
      if (mComicBook.PublishedDate != 0) {
        mPublishedDateEdit.setText(DateUtils.formatDateForDisplay(mComicBook.PublishedDate));
      } else {
        mPublishedDateEdit.setText(DateUtils.formatDateForDisplay(Calendar.getInstance().getTimeInMillis()));
      }

      TextView mSeriesText = view.findViewById(R.id.comic_book_text_series_value);
      mSeriesText.setText(mComicSeries.SeriesName);
      TextView mPublisherText = view.findViewById(R.id.comic_book_text_publisher_value);
      mPublisherText.setText(mComicPublisher.Name);

      TextView mVolumeText = view.findViewById(R.id.comic_book_text_volume_value);
      mVolumeText.setText(String.valueOf(mComicSeries.Volume));

      TextView mIssueText = view.findViewById(R.id.comic_book_text_issue_value);
      mIssueText.setText(String.valueOf(mComicBook.IssueNumber));
      TextView mProductCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
      mProductCodeText.setText(mComicBook.SeriesId);

      mOwnedToggle = view.findViewById(R.id.comic_book_toggle_owned);
      mOwnedToggle.setChecked(mComicBook.OwnedState);
      mReadToggle = view.findViewById(R.id.comic_book_toggle_read);
      mReadToggle.setChecked(mComicBook.ReadState);

      Button addToLibraryButton = view.findViewById(R.id.comic_book_button_add);
      Button updateButton = view.findViewById(R.id.comic_book_button_update);
      Button removeFromLibraryButton = view.findViewById(R.id.comic_book_button_remove);

      if (mComicBook.AddedDate == 0) {
        updateButton.setVisibility(View.GONE);
        removeFromLibraryButton.setVisibility(View.GONE);
        addToLibraryButton.setVisibility(View.VISIBLE);
        addToLibraryButton.setOnClickListener(v -> {

          // TODO: add validation
          mCallback.onComicBookStarted();
          ComicBook updatedBook = new ComicBook(mComicBook);
          updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
          updatedBook.OwnedState = mOwnedToggle.isChecked();
          updatedBook.PublishedDate = DateUtils.fromString(mPublishedDateEdit.getText().toString());
          updatedBook.ReadState = mReadToggle.isChecked();
          updatedBook.Title = mTitleEdit.getText().toString();

          String comicBookQueryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, updatedBook.getFullId());
          Trace comicBookTrace = FirebasePerformance.getInstance().newTrace("set_comic_book");
          comicBookTrace.start();
          FirebaseFirestore.getInstance().document(comicBookQueryPath).set(updatedBook, SetOptions.merge())
            .addOnCompleteListener(task -> {

              if (task.isSuccessful()) {
                mCallback.onComicBookAddedToLibrary(updatedBook);
                comicBookTrace.incrementMetric("comic_book_add", 1);
              } else {
                LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", comicBookQueryPath);
                if (task.getException() != null) {
                  Crashlytics.logException(task.getException());
                }

                mCallback.onComicBookAddedToLibrary(null);
                comicBookTrace.incrementMetric("comic_book_err", 1);
              }

              comicBookTrace.stop();
            });
        });
      } else {
        addToLibraryButton.setVisibility(View.GONE);
        updateButton.setVisibility(View.VISIBLE);
        updateButton.setOnClickListener(v -> {

          mCallback.onComicBookStarted();
          ComicBook updatedBook = new ComicBook(mComicBook);
          updatedBook.OwnedState = mOwnedToggle.isChecked();
          updatedBook.PublishedDate = mComicBook.PublishedDate;
          updatedBook.ReadState = mReadToggle.isChecked();
          updatedBook.Title = mTitleEdit.getText().toString();
          updatedBook.ModifiedDate = Calendar.getInstance().getTimeInMillis();

          String comicBookQueryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, updatedBook.getFullId());
          Trace comicBookTrace = FirebasePerformance.getInstance().newTrace("set_comic_book");
          comicBookTrace.start();
          FirebaseFirestore.getInstance().document(comicBookQueryPath).set(updatedBook, SetOptions.merge())
            .addOnCompleteListener(task -> {

              if (task.isSuccessful()) {
                mCallback.onComicBookUpdated(updatedBook);
                comicBookTrace.incrementMetric("comic_book_update", 1);
              } else {
                LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", comicBookQueryPath);
                if (task.getException() != null) {
                  Crashlytics.logException(task.getException());
                }

                mCallback.onComicBookUpdated(null);
                comicBookTrace.incrementMetric("comic_book_err", 1);
              }

              comicBookTrace.stop();
            });
        });

        removeFromLibraryButton.setOnClickListener(v -> {

          mCallback.onComicBookStarted();
          if (getActivity() != null) {
            String message = String.format(Locale.US, getString(R.string.remove_book_message), mComicBook.Title);
            if (mComicBook.Title.isEmpty()) {
              message = "Remove comic book from your library?";
            }

            AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
              .setMessage(message)
              .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                String queryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, mComicBook.getFullId());
                Trace comicBookTrace = FirebasePerformance.getInstance().newTrace("del_comic_book");
                comicBookTrace.start();
                FirebaseFirestore.getInstance().document(queryPath).delete().addOnCompleteListener(task -> {

                  if (task.isSuccessful()) {
                    mCallback.onComicBookRemoved(mComicBook);
                    comicBookTrace.incrementMetric("comic_book_del", 1);
                  } else {
                    LogUtils.error(TAG, "Failed to remove book from user's library: %s", queryPath);
                    if (task.getException() != null) {
                      Crashlytics.logException(task.getException());
                    }

                    mCallback.onComicBookRemoved(null);
                    comicBookTrace.incrementMetric("comic_book_err", 1);
                  }
                });

                comicBookTrace.stop();
              })
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

      mCallback.onComicBookInit(true);
    }

    return view;
  }
}

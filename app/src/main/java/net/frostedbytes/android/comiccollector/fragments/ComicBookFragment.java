package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;

import java.util.Locale;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;

public class ComicBookFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookFragment";

  public interface OnComicBookListener {

    void onComicBookActionComplete(String message);

    void onComicBookAddedToLibrary(ComicBook comicBook);

    void onComicBookInit(boolean isSuccessful);

    void onComicBookRemoved(ComicBook comicBook);
  }

  private OnComicBookListener mCallback;

  private TextView mSeriesText;
  private TextView mPublisherText;
  private EditText mPublishedDateEdit;
  private TextView mVolumeText;
  private TextView mIssueText;
  private TextView mProductCodeText;
  private ToggleButton mOwnedToggle;
  private ToggleButton mReadToggle;
  private EditText mTitleEdit;
  private Button mSaveButton;
  private Button mRemoveButton;

  private ComicBookDetails mComicBook;

  public static ComicBookFragment newInstance(ComicBookDetails comicBook) {

    LogUtils.debug(TAG, "++newInstance(%s)", comicBook.toString());
    ComicBookFragment fragment = new ComicBookFragment();
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
      mCallback = (OnComicBookListener) context;
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
      mComicBook = (ComicBookDetails)arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_comic_book, container, false);
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
    mTitleEdit = view.findViewById(R.id.comic_book_edit_title);
    mSeriesText = view.findViewById(R.id.comic_book_text_series_value);
    mPublisherText = view.findViewById(R.id.comic_book_text_publisher_value);
    mPublishedDateEdit = view.findViewById(R.id.comic_book_edit_published_date);
    mVolumeText = view.findViewById(R.id.comic_book_text_volume_value);
    mIssueText = view.findViewById(R.id.comic_book_text_issue_value);
    mProductCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
    mOwnedToggle = view.findViewById(R.id.comic_book_toggle_owned);
    mReadToggle = view.findViewById(R.id.comic_book_toggle_read);
    mSaveButton = view.findViewById(R.id.comic_book_button_save);
    mRemoveButton = view.findViewById(R.id.comic_book_button_remove);
    updateUI(mComicBook);
  }

  /*
    Private Method(s)
   */
  private void updateUI(ComicBookDetails comicBook) {

    LogUtils.debug(TAG, "++updateUI()");
    if (comicBook == null) {
      mCallback.onComicBookInit(false);
    } else {
      mTitleEdit.setText(comicBook.Title);
      if (!comicBook.Published.equals(BaseActivity.DEFAULT_PUBLISHED_DATE) &&
        comicBook.Published.length() == BaseActivity.DEFAULT_PUBLISHED_DATE.length()) {
        mPublishedDateEdit.setText(comicBook.Published);
      }

      if (comicBook.PublisherName.length() > 0) {
        mPublisherText.setText(comicBook.PublisherName);
      } else {
        mPublisherText.setText(getString(R.string.placeholder));
      }

      if (comicBook.SeriesTitle.length() > 0) {
        mSeriesText.setText(comicBook.SeriesTitle);
      } else {
        mSeriesText.setText(getString(R.string.placeholder));
      }

      mVolumeText.setText(String.valueOf(comicBook.Volume));
      mIssueText.setText(String.valueOf(comicBook.IssueNumber));
      mProductCodeText.setText(comicBook.ProductCode);
      mOwnedToggle.setChecked(comicBook.IsOwned);
      mReadToggle.setChecked(comicBook.IsRead);

      mSaveButton.setOnClickListener(v -> {

        ComicBook updatedBook = new ComicBook();
        updatedBook.parseProductCode(mComicBook.Id);
        updatedBook.IsOwned = mOwnedToggle.isChecked();
        updatedBook.IsRead = mReadToggle.isChecked();
        updatedBook.Title = mTitleEdit.getText().toString();
        updatedBook.PublishedDate = mPublishedDateEdit.getText().toString();

        if (updatedBook.isValid()) {
          mCallback.onComicBookAddedToLibrary(updatedBook);
        } else {
          // -??? mCallback.onComicBookActionComplete();
        }
      });

      mRemoveButton.setVisibility(View.INVISIBLE);
    }
  }
}

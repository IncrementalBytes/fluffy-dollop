package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.DateUtils;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + ComicBookFragment.class.getSimpleName();

    public interface OnComicBookListener {

        void onComicBookActionComplete(String message);

        void onComicBookAddedToLibrary(ComicBook comicBook);

        void onComicBookInit(boolean isSuccessful);

        void onComicBookRemoved(ComicBook comicBook);

        void onComicBookStarted();

        void onComicBookUpdated(ComicBook comicBook);
    }

    private OnComicBookListener mCallback;

    private TextView mPublisherText;
    private TextView mVolumeText;
    private EditText mTitleText;
    private EditText mPublishedDateText;
    private TextView mIssueText;
    private TextView mProductCodeText;
    private ToggleButton mOwnedToggle;
    private ToggleButton mWishlistToggle;

    private ComicBook mComicBook;
    private ComicSeries mTargetSeries;
    private String mUserId;

    public Map<String, ComicSeries> ComicSeries;

    public static ComicBookFragment newInstance(String userId, ComicBook comicBook) {

        LogUtils.debug(TAG, "++newInstance(%s, %s)", userId, comicBook.toString());
        ComicBookFragment fragment = new ComicBookFragment();
        Bundle args = new Bundle();
        args.putString(BaseActivity.ARG_USER_ID, userId);
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
            mCallback = (OnComicBookListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mComicBook = arguments.getParcelable(BaseActivity.ARG_COMIC_BOOK);
            mUserId = arguments.getString(BaseActivity.ARG_USER_ID);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_comic_book, container, false);

        mPublisherText = view.findViewById(R.id.comic_book_text_publisher_value);
        mVolumeText = view.findViewById(R.id.comic_book_text_volume_value);
        mTitleText = view.findViewById(R.id.comic_book_edit_title);
        mPublishedDateText = view.findViewById(R.id.comic_book_edit_published_date);
        mIssueText = view.findViewById(R.id.comic_book_text_issue_value);
        mProductCodeText = view.findViewById(R.id.comic_book_text_product_code_value);
        mOwnedToggle = view.findViewById(R.id.comic_book_toggle_owned);
        mWishlistToggle = view.findViewById(R.id.comic_book_toggle_wishlist);

        Spinner seriesSpinner = view.findViewById(R.id.comic_book_spinner_series);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getActivity(),
            android.R.layout.simple_spinner_item,
            ComicSeries.keySet().toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seriesSpinner.setAdapter(adapter);
        seriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                int index = 0;
                for (ComicSeries series : ComicSeries.values()) {
                    if (index == position) {
                        mTargetSeries = series;
                        mComicBook.SeriesCode = mTargetSeries.Code;
                        mComicBook.SeriesName = mTargetSeries.Name;
                        mComicBook.Publisher = mTargetSeries.Publisher;
                        mComicBook.Volume = mTargetSeries.Volume;
                        updateUI();
                        break;
                    }

                    index++;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int index = 0;
        for (ComicSeries series : ComicSeries.values()) {
            if (mComicBook.SeriesCode.equals(series.Code)) {
                seriesSpinner.setSelection(index);
                mTargetSeries = series;
                mComicBook.SeriesCode = mTargetSeries.Code;
                mComicBook.SeriesName = mTargetSeries.Name;
                mComicBook.Publisher = mTargetSeries.Publisher;
                mComicBook.Volume = mTargetSeries.Volume;
                updateUI();
                break;
            }

            index++;
        }

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
                ComicBook updatedBook = new ComicBook();
                updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
                updatedBook.IsOwned = mOwnedToggle.isChecked();
                updatedBook.IssueCode = mComicBook.IssueCode;
                updatedBook.OnWishlist = mWishlistToggle.isChecked();
                updatedBook.PublishedDate = DateUtils.fromString(mPublishedDateText.getText().toString());
                updatedBook.Publisher = mTargetSeries.Publisher;
                updatedBook.SeriesCode = mTargetSeries.Code;
                updatedBook.SeriesName = mTargetSeries.Name;
                updatedBook.Title = mTitleText.getText().toString();
                updatedBook.Volume = mTargetSeries.Volume;

                String comicBookQueryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, updatedBook.getUniqueId());
                FirebaseFirestore.getInstance().document(comicBookQueryPath).set(updatedBook, SetOptions.merge())
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mCallback.onComicBookAddedToLibrary(updatedBook);
                        } else {
                            LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", comicBookQueryPath);
                            if (task.getException() != null) {
//                                Crashlytics.logException(task.getException());
                            }

                            mCallback.onComicBookAddedToLibrary(null);
                        }
                    });
            });
        } else {
            addToLibraryButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            updateButton.setOnClickListener(v -> {

                mCallback.onComicBookStarted();
                ComicBook updatedBook = new ComicBook();
                updatedBook.AddedDate = mComicBook.AddedDate;
                updatedBook.IsOwned = mOwnedToggle.isChecked();
                updatedBook.IssueCode = mComicBook.IssueCode;
                updatedBook.OnWishlist = mWishlistToggle.isChecked();
                updatedBook.SeriesCode = mTargetSeries.Code;
                updatedBook.SeriesName = mTargetSeries.Name;
                updatedBook.PublishedDate = mComicBook.PublishedDate;
                updatedBook.Publisher = mComicBook.Publisher;
                updatedBook.Title = mTitleText.getText().toString();
                updatedBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
                updatedBook.Volume = mTargetSeries.Volume;

                String comicBookQueryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, updatedBook.getUniqueId());
                FirebaseFirestore.getInstance().document(comicBookQueryPath).set(updatedBook, SetOptions.merge())
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mCallback.onComicBookUpdated(updatedBook);
                        } else {
                            LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", comicBookQueryPath);
                            if (task.getException() != null) {
//                                Crashlytics.logException(task.getException());
                            }

                            mCallback.onComicBookUpdated(null);
                        }
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

                            String queryPath = PathUtils.combine(User.ROOT, mUserId, ComicBook.ROOT, mComicBook.getUniqueId());
                            FirebaseFirestore.getInstance().document(queryPath).delete().addOnCompleteListener(task -> {

                                if (task.isSuccessful()) {
                                    mCallback.onComicBookRemoved(mComicBook);
                                } else {
                                    LogUtils.error(TAG, "Failed to remove book from user's library: %s", queryPath);
                                    if (task.getException() != null) {
//                                        Crashlytics.logException(task.getException());
                                    }

                                    mCallback.onComicBookRemoved(null);
                                }
                            });
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
        return view;
    }

    private void updateUI() {

        LogUtils.debug(TAG, "++updateUI()");
        mPublisherText.setText(mComicBook.Publisher);
        mVolumeText.setText(String.valueOf(mComicBook.Volume));
        mTitleText.setText(mComicBook.Title);
        mPublishedDateText.setText(DateUtils.formatDateForDisplay(mComicBook.PublishedDate));
        mIssueText.setText(String.valueOf(mComicBook.getIssueNumber()));
        mProductCodeText.setText(mComicBook.getUniqueId());
        mOwnedToggle.setChecked(mComicBook.IsOwned);
        mWishlistToggle.setChecked(mComicBook.OnWishlist);
    }
}

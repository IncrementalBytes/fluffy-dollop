package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BASE_TAG + ManualSearchFragment.class.getSimpleName();

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(String seriesCode);

    void onManualSearchActionComplete(String seriesCode, String issueCode);

    void onManualSearchListItemSelected(ComicBook comicBook);

    void onManualSearchListPopulated(int size);
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueEdit;
  private EditText mSeriesEdit;
  private RecyclerView mRecyclerView;

  private ArrayList<ComicBook> mComicBooks;
  private String mSeriesCode;

  public static ManualSearchFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return ManualSearchFragment.newInstance("");
  }

  public static ManualSearchFragment newInstance(String seriesCode) {

    LogUtils.debug(TAG, "++newInstance(%s)", seriesCode);
    return ManualSearchFragment.newInstance(seriesCode, new ArrayList<>());
  }

  public static ManualSearchFragment newInstance(String seriesCode, ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++newInstance(%s, %d)", seriesCode, comicBooks.size());
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_COMIC_SERIES_CODE, seriesCode);
    args.putParcelableArrayList(BaseActivity.ARG_COMIC_BOOK_LIST, comicBooks);
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
      mComicBooks = arguments.getParcelableArrayList(BaseActivity.ARG_COMIC_BOOK_LIST);
      mSeriesCode = arguments.getString(BaseActivity.ARG_COMIC_SERIES_CODE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_manual_search, container, false);

    mRecyclerView = view.findViewById(R.id.manual_search_list_view);
    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);

    Button cancelButton = view.findViewById(R.id.manual_search_button_cancel);
    cancelButton.setOnClickListener(v ->
      mCallback.onManualSearchActionComplete("", ""));
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {
      if (mSeriesCode.isEmpty()) {
        mCallback.onManualSearchActionComplete(mSeriesEdit.getText().toString());
      } else {
        mCallback.onManualSearchActionComplete(mSeriesEdit.getText().toString(), mIssueEdit.getText().toString());
      }
    });

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
    if (!mSeriesCode.isEmpty()) {
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
    } else {
      TextView issueText = view.findViewById(R.id.manual_search_text_issue);
      issueText.setVisibility(View.INVISIBLE);
      mIssueEdit.setVisibility(View.INVISIBLE);
    }

    updateUI();
    return view;
  }

  /*
      Private Method(s)
     */
  private void updateUI() {

    if (mComicBooks == null || mComicBooks.size() == 0) {
      mCallback.onManualSearchListPopulated(0);
    } else {
      LogUtils.debug(TAG, "++updateUI()");
      mComicBooks.sort(new SortUtils.ByPublicationDate());
      ComicBookAdapter comicAdapter = new ComicBookAdapter(mComicBooks);
      mRecyclerView.setAdapter(comicAdapter);
      mCallback.onManualSearchListPopulated(comicAdapter.getItemCount());
    }
  }

  private void validateAll() {

    if (mSeriesCode.isEmpty()) {
      if (mSeriesEdit.getText().toString().length() == BaseActivity.DEFAULT_SERIES_CODE.length() &&
        !mSeriesEdit.getText().toString().equals(BaseActivity.DEFAULT_SERIES_CODE)) {
        mContinueButton.setEnabled(true);
      } else {
        mContinueButton.setEnabled(false);
      }
    } else {
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

  /**
   * Adapter class for ComicBook objects
   */
  private class ComicBookAdapter extends RecyclerView.Adapter<ComicHolder> {

    private final List<ComicBook> mComicBooks;

    ComicBookAdapter(List<ComicBook> comicBooks) {

      mComicBooks = comicBooks;
    }

    @NonNull
    @Override
    public ComicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new ComicHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicHolder holder, int position) {

      ComicBook comicBook = mComicBooks.get(position);
      holder.bind(comicBook);
    }

    @Override
    public int getItemCount() {
      return mComicBooks.size();
    }
  }

  /**
   * Holder class for Comic objects
   */
  private class ComicHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mIssueTextView;
    private final ImageView mOwnImage;
    private final TextView mPublisherTextView;
    private final TextView mSeriesNameTextView;
    private final TextView mTitleTextView;
    private final TextView mVolumeTextView;

    private ComicBook mComicBook;

    ComicHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_book_item, parent, false));

      mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
      mPublisherTextView = itemView.findViewById(R.id.comic_item_text_publisher);
      mOwnImage = itemView.findViewById(R.id.comic_item_image_own);
      mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
      mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);
      mVolumeTextView = itemView.findViewById(R.id.comic_item_text_volume_value);

      itemView.setOnClickListener(this);
    }

    void bind(ComicBook comicBook) {

      mComicBook = comicBook;

      mPublisherTextView.setText(mComicBook.Publisher);
      if (mComicBook.OwnedState) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_owned_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_wishlist_dark, null));
      }

      mSeriesNameTextView.setText(mComicBook.SeriesName);
      mTitleTextView.setText(mComicBook.Title);
      mVolumeTextView.setText(String.valueOf(mComicBook.Volume));
      mIssueTextView.setText(String.valueOf(mComicBook.IssueNumber));
    }

    @Override
    public void onClick(View view) {

      LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
      mCallback.onManualSearchListItemSelected(mComicBook);
    }
  }
}

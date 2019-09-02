package net.frostedbytes.android.comiccollector.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

import java.util.Locale;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.viewmodel.CollectorViewModel;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicBookListFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicBookListFragment";

  public interface OnComicBookListListener {

    void onComicListActionComplete(String message);

    void onComicListAddBook();

    void onComicListDeleteBook();

    void onComicListItemSelected(ComicBookDetails comicBook);

    void onComicListPopulated(int size);
  }

  private OnComicBookListListener mCallback;

  private CollectorViewModel mCollectorViewModel;

  private RecyclerView mRecyclerView;

  private String mProductCode;

  public static ComicBookListFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new ComicBookListFragment();
  }

  public static ComicBookListFragment newInstance(String productCode) {

    LogUtils.debug(TAG, "++newInstance(%s)", productCode);
    ComicBookListFragment fragment = new ComicBookListFragment();
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
      mCallback = (OnComicBookListListener) context;
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
    mCollectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
    if (mProductCode != null && mProductCode.length() > 0) {
      mCollectorViewModel.getComicBooksByProductCode(mProductCode).observe(this, bookList -> {

        ComicBookAdapter comicAdapter = new ComicBookAdapter(bookList);
        mRecyclerView.setAdapter(comicAdapter);
        mCallback.onComicListPopulated(bookList.size());
      });
    } else {
      mCollectorViewModel.getComicBooks().observe(this, bookList -> {

        ComicBookAdapter comicAdapter = new ComicBookAdapter(bookList);
        mRecyclerView.setAdapter(comicAdapter);
        mCallback.onComicListPopulated(bookList.size());
      });
    }

    return inflater.inflate(R.layout.fragment_comic_book_list, container, false);
  }


  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
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
    FloatingActionButton addButton = view.findViewById(R.id.comic_fab_add);
    mRecyclerView = view.findViewById(R.id.comic_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);
    addButton.setOnClickListener(pickView -> mCallback.onComicListAddBook());
  }

  /**
   * Adapter class for ComicBook objects
   */
  private class ComicBookAdapter extends RecyclerView.Adapter<ComicHolder> {

    private final List<ComicBookDetails> mComicBooks;

    ComicBookAdapter(List<ComicBookDetails> comicBooks) {

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

      ComicBookDetails comicBook = mComicBooks.get(position);
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

    private final ImageView mDeleteImage;
    private final TextView mIssueTextView;
    private final ImageView mOwnImage;
    private final ImageView mReadImage;
    private final TextView mSeriesNameTextView;
    private final TextView mTitleTextView;

    private ComicBookDetails mComicBook;

    ComicHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_book_item, parent, false));

      mDeleteImage = itemView.findViewById(R.id.comic_item_image_delete);
      mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
      mOwnImage = itemView.findViewById(R.id.comic_item_image_own);
      mReadImage = itemView.findViewById(R.id.comic_item_image_read);
      mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
      mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);

      itemView.setOnClickListener(this);
    }

    void bind(ComicBookDetails comicBook) {

      mComicBook = comicBook;

      mDeleteImage.setOnClickListener(v -> {
        if (getActivity() != null) {
          String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mComicBook.Title);
          if (mComicBook.Title.isEmpty()) {
            message = getString(R.string.remove_book_message);
          }

          AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
              mCollectorViewModel.deleteComicBookById(mComicBook.Id);
              mCallback.onComicListDeleteBook();
            })
            .setNegativeButton(android.R.string.no, null)
            .create();
          removeBookDialog.show();
        } else {
          mCallback.onComicListActionComplete(getString(R.string.err_remove_comic_book));
        }
      });

      if (mComicBook.IsOwned) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      if (mComicBook.IsRead) {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      mSeriesNameTextView.setText(mComicBook.SeriesTitle);
      mTitleTextView.setText(mComicBook.Title);
      mIssueTextView.setText(String.valueOf(mComicBook.IssueNumber));
    }

    @Override
    public void onClick(View view) {

      LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
      mCallback.onComicListItemSelected(mComicBook);
    }
  }
}

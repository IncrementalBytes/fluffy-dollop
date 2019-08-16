package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
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

import java.util.HashMap;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicBookListFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ComicBookListFragment";

  public interface OnComicBookListListener {

    void onComicListAddBook();

    void onComicListItemSelected(ComicBook comicBook);

    void onComicListPopulated(int size);
  }

  private OnComicBookListListener mCallback;

  private RecyclerView mRecyclerView;

  private ArrayList<ComicBook> mComicBooks;
  private HashMap<String, ComicSeries> mComicSeries;

  public static ComicBookListFragment newInstance(ComicSeries comicSeries) {

    LogUtils.debug(TAG, "++newInstance(%s)", comicSeries.toString());
    HashMap<String, ComicSeries> series = new HashMap<>();
    series.put(comicSeries.getProductId(), comicSeries);
    ComicBookListFragment fragment = new ComicBookListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_SERIES, series);
    fragment.setArguments(args);
    return fragment;
  }

  public static ComicBookListFragment newInstance(HashMap<String, ComicSeries> comicSeries) {

    LogUtils.debug(TAG, "++newInstance(%d)", comicSeries.size());
    ComicBookListFragment fragment = new ComicBookListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_COMIC_SERIES, comicSeries);
    fragment.setArguments(args);
    return fragment;
  }

  /*
    Fragment Override(s)
   */
  @SuppressWarnings("unchecked")
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

    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicSeries = (HashMap<String, ComicSeries>) arguments.getSerializable(BaseActivity.ARG_COMIC_SERIES);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_comic_book_list, container, false);

    FloatingActionButton addButton = view.findViewById(R.id.comic_fab_add);
    mRecyclerView = view.findViewById(R.id.comic_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);
    addButton.setOnClickListener(pickView -> mCallback.onComicListAddBook());

    mComicBooks = new ArrayList<>();
    for (ComicSeries series : mComicSeries.values()) {
      mComicBooks.addAll(series.ComicBooks);
    }

    updateUI();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mComicSeries = null;
  }

  /*
    Private Method(s)
   */
  private void updateUI() {

    if (mComicBooks == null || mComicBooks.size() == 0) {
      mCallback.onComicListPopulated(0);
    } else {
      LogUtils.debug(TAG, "++updateUI()");
      mComicBooks.sort(new SortUtils.ByIssueNumber());
      ComicBookAdapter comicAdapter = new ComicBookAdapter(mComicBooks);
      mRecyclerView.setAdapter(comicAdapter);
      mCallback.onComicListPopulated(comicAdapter.getItemCount());
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
    private final ImageView mReadImage;
    private final TextView mSeriesNameTextView;
    private final TextView mTitleTextView;

    private ComicBook mComicBook;

    ComicHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.comic_book_item, parent, false));

      mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
      mOwnImage = itemView.findViewById(R.id.comic_item_image_own);
      mReadImage = itemView.findViewById(R.id.comic_item_image_read);
      mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
      mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);

      itemView.setOnClickListener(this);
    }

    void bind(ComicBook comicBook) {

      mComicBook = comicBook;
      if (mComicBook.OwnedState) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      if (mComicBook.ReadState) {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      if (mComicSeries != null) {
        ComicSeries series = mComicSeries.get(mComicBook.getProductId());
        if (series != null) {
          mSeriesNameTextView.setText(
            String.format(
              Locale.US,
              "%s%s",
              series.SeriesName,
              series.IsFlagged ? " (pending)" : ""));
        } else {
          mSeriesNameTextView.setText("N/A");
        }
      } else {
        mSeriesNameTextView.setText("N/A");
      }

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

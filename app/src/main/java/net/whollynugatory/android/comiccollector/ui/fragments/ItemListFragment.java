package net.whollynugatory.android.comiccollector.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.ComicBookViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ItemListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ItemListFragment";

  public enum ItemType {
    Comics,
    Series,
    Categories
  }

  public interface OnItemListListener {

    void onItemListAddComicBook();

    void onItemListSeriesSelected(String series);

    void onItemListCategorySelected(String category);

    void onItemListPopulated(int size);
  }

  private OnItemListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private ComicBookViewModel mComicBookViewModel;

  private ComicBookEntity mComicBookEntity;
  private String mItemName;
  private ItemType mItemType;

  public static ItemListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return newInstance(ItemType.Comics, "");
  }

  public static ItemListFragment newInstance(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++newInstance(BookEntity)");
    ItemListFragment fragment = new ItemListFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_COMIC_BOOK, comicBookEntity);
    fragment.setArguments(arguments);
    return fragment;
  }

  public static ItemListFragment newInstance(ItemType itemType) {

    Log.d(TAG, "++newInstance(ItemType)");
    return newInstance(itemType, "");
  }

  public static ItemListFragment newInstance(ItemType itemType, String itemName) {

    Log.d(TAG, "++newInstance(ItemType, String)");
    ItemListFragment fragment = new ItemListFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_LIST_TYPE, itemType);
    arguments.putString(BaseActivity.ARG_ITEM_NAME, itemName);
    fragment.setArguments(arguments);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    switch (mItemType) {
      case Comics:
        ComicBookEntityAdapter comicBookEntityAdapter = new ComicBookEntityAdapter(getContext());
        mRecyclerView.setAdapter(comicBookEntityAdapter);
        if (mComicBookEntity != null) {
          comicBookEntityAdapter.setComicBookEntityList(Collections.singletonList(mComicBookEntity));
        } else {
          mComicBookViewModel.getRecent().observe(this, comicBookEntityAdapter::setComicBookEntityList);
        }

        break;
      case Series:
        // TODO: update
//        if (mItemName != null && mItemName.length() > 0) {
//          ComicBookEntityAdapter specificSeriesAdapter = new ComicBookEntityAdapter(getContext());
//          mRecyclerView.setAdapter(specificSeriesAdapter);
//          mComicBookViewModel.getAllBySeries(mItemName).observe(this, specificSeriesAdapter::setComicBookEntityList);
//        } else {
//          SeriesAdapter seriesAdapter = new SeriesAdapter(getContext());
//          mRecyclerView.setAdapter(seriesAdapter);
//          mComicBookViewModel.getSummaryBySeries().observe(this, seriesAdapter::setSeriesList);
//        }

        break;
      case Categories:
        // TODO: update
//        if (mItemName != null && mItemName.length() > 0) {
//          ComicBookEntityAdapter specificCategoryAdapter = new ComicBookEntityAdapter(getContext());
//          mRecyclerView.setAdapter(specificCategoryAdapter);
//          mComicBookViewModel.getAllByCategory(mItemName).observe(this, specificCategoryAdapter::setComicBookEntityList);
//        } else {
//          BookCategoryAdapter bookCategoryAdapter = new BookCategoryAdapter(getContext());
//          mRecyclerView.setAdapter(bookCategoryAdapter);
//          mComicBookViewModel.getSummaryByCategories().observe(this, bookCategoryAdapter::setCategoryList);
//        }

        break;
    }

    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onItemListAddComicBook());
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnItemListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    mItemType = ItemType.Comics;
    if (arguments != null) {
      if (arguments.containsKey(BaseActivity.ARG_LIST_TYPE)) {
        mItemType = (ItemType) arguments.getSerializable(BaseActivity.ARG_LIST_TYPE);
      }

      if (arguments.containsKey(BaseActivity.ARG_ITEM_NAME)) {
        mItemName = arguments.getString(BaseActivity.ARG_ITEM_NAME);
      }

      if (arguments.containsKey(BaseActivity.ARG_COMIC_BOOK)) {
        mComicBookEntity = (ComicBookEntity) arguments.getSerializable(BaseActivity.ARG_COMIC_BOOK);
      }
    }

    mComicBookViewModel = ViewModelProviders.of(this).get(ComicBookViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_list, container, false);
    mAddButton = view.findViewById(R.id.fab_add);
    mRecyclerView = view.findViewById(R.id.list_view);
    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
  }

  /*
  Adapter class for BookCategory objects
*/
//  private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryHolder> {
//
//    /*
//      Holder class for Category objects
//     */
//    class CategoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
//
//      private final TextView mCategoryTextView;
//      private final TextView mBookCountTextView;
//
//      private BookCategory mBookCategory;
//
//      BookCategoryHolder(View itemView) {
//        super(itemView);
//
//        mCategoryTextView = itemView.findViewById(R.id.category_item_name);
//        mBookCountTextView = itemView.findViewById(R.id.category_item_count);
//
//        itemView.setOnClickListener(this);
//      }
//
//      void bind(BookCategory bookCategory) {
//
//        mBookCategory = bookCategory;
//
//        if (mBookCategory != null) {
//          mCategoryTextView.setText(mBookCategory.CategoryName);
//          mBookCountTextView.setText(String.format(getString(R.string.books_within_category), mBookCategory.BookCount));
//        }
//      }
//
//      @Override
//      public void onClick(View view) {
//
//        Log.d(TAG, "++BookCategoryHolder::onClick(View)");
//        mCallback.onItemListCategorySelected(mBookCategory.CategoryName);
//      }
//    }
//
//    private final LayoutInflater mInflater;
//    private List<BookCategory> mBookCategoryList;
//
//    BookCategoryAdapter(Context context) {
//
//      mInflater = LayoutInflater.from(context);
//    }
//
//    @NonNull
//    @Override
//    public BookCategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//
//      View itemView = mInflater.inflate(R.layout.item_category, parent, false);
//      return new BookCategoryHolder(itemView);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull BookCategoryHolder holder, int position) {
//
//      if (mBookCategoryList != null) {
//        BookCategory bookCategory = mBookCategoryList.get(position);
//        holder.bind(bookCategory);
//      } else {
//        // No books!
//      }
//    }
//
//    @Override
//    public int getItemCount() {
//
//      if (mBookCategoryList != null) {
//        return mBookCategoryList.size();
//      } else {
//        return 0;
//      }
//    }
//
//    void setBookCategoryList(List<BookCategory> bookCategoryList) {
//
//      Log.d(TAG, "++setBookCategoryList(List<BookCategory>)");
//      mBookCategoryList = bookCategoryList;
//      notifyDataSetChanged();
//    }
//  }

  /*
  Adapter class for ComicBookEntity objects
 */
  private class ComicBookEntityAdapter extends RecyclerView.Adapter<ComicBookEntityAdapter.ComicBookEntityHolder> {

    /*
      Holder class for ComicBookEntity objects
     */
    class ComicBookEntityHolder extends RecyclerView.ViewHolder {

      private final ImageView mDeleteImage;
      private final TextView mIssueTextView;
      private final Switch mOwnSwitch;
      private final Switch mReadSwitch;
      private final TextView mSeriesNameTextView;
      private final TextView mTitleTextView;

      private ComicBookEntity mComicBook;

      ComicBookEntityHolder(View itemView) {
        super(itemView);

        mDeleteImage = itemView.findViewById(R.id.comic_item_image_delete);
        mIssueTextView = itemView.findViewById(R.id.comic_item_text_issue_value);
        mOwnSwitch = itemView.findViewById(R.id.comic_item_switch_own);
        mReadSwitch = itemView.findViewById(R.id.comic_item_switch_read);
        mSeriesNameTextView = itemView.findViewById(R.id.comic_item_text_series);
        mTitleTextView = itemView.findViewById(R.id.comic_item_text_title);

        mOwnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

          mComicBook.IsOwned = isChecked;
          mComicBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
          mComicBookViewModel.update(mComicBook);
          mOwnSwitch.setText(isChecked ? getString(R.string.owned) : getString(R.string.not_owned));
        });

        mReadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

          mComicBook.HasRead = isChecked;
          mComicBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
          mComicBookViewModel.update(mComicBook);
          mReadSwitch.setText(isChecked ? getString(R.string.read) : getString(R.string.unread));
        });
      }

      void bind(ComicBookEntity comicBookEntity) {

        mComicBook = comicBookEntity;

        if (mComicBook != null) {
          mDeleteImage.setOnClickListener(v -> {
            if (getActivity() != null) {
              String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mComicBook.Title);
              AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mComicBookViewModel.delete(mComicBook.Id)) // TODO: double-check
                .setNegativeButton(android.R.string.no, null)
                .create();
              removeBookDialog.show();
            } else {
              Log.w(TAG, "Unable to remove book at this time.");
            }
          });

          mOwnSwitch.setText(mComicBook.IsOwned ? getString(R.string.owned) : getString(R.string.not_owned));
          mOwnSwitch.setChecked(mComicBook.IsOwned);
          mReadSwitch.setText(mComicBook.HasRead ? getString(R.string.read) : getString(R.string.unread));
          mReadSwitch.setChecked(mComicBook.HasRead);

//      mSeriesNameTextView.setText(mComicBook.SeriesTitle);
          mTitleTextView.setText(mComicBook.Title);
//      mIssueTextView.setText(String.valueOf(mComicBook.IssueNumber));
        }
      }
    }

    private final LayoutInflater mInflater;
    private List<ComicBookEntity> mComicBookEntityList;

    ComicBookEntityAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ComicBookEntityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.comic_book_item, parent, false);
      return new ComicBookEntityHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicBookEntityHolder holder, int position) {

      if (mComicBookEntityList != null) {
        ComicBookEntity comicBookEntity = mComicBookEntityList.get(position);
        holder.bind(comicBookEntity);
      } else {
        // No books!
      }
    }

    @Override
    public int getItemCount() {

      if (mComicBookEntityList != null) {
        return mComicBookEntityList.size();
      } else {
        return 0;
      }
    }

    void setComicBookEntityList(List<ComicBookEntity> comicBookEntityList) {

      Log.d(TAG, "++setComicBookEntityList(List<ComicBookEntity>)");
      mComicBookEntityList = new ArrayList<>(comicBookEntityList);
      mCallback.onItemListPopulated(comicBookEntityList.size());
      notifyDataSetChanged();
    }
  }

  /*
    Adapter class for SeriesAdapter objects
  */
//  private class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesHolder> {
//
//    /*
//      Holder class for BookAuthor objects
//     */
//    class BookAuthorHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
//
//      private final TextView mAuthorTextView;
//      private final TextView mBookCountTextView;
//
//      private BookAuthor mBookAuthor;
//
//      BookAuthorHolder(View itemView) {
//        super(itemView);
//
//        mAuthorTextView = itemView.findViewById(R.id.author_item_name);
//        mBookCountTextView = itemView.findViewById(R.id.author_item_count);
//
//        itemView.setOnClickListener(this);
//      }
//
//      void bind(BookAuthor bookAuthor) {
//
//        mBookAuthor = bookAuthor;
//
//        if (mBookAuthor != null) {
//          mAuthorTextView.setText(mBookAuthor.AuthorName);
//          mBookCountTextView.setText(String.format(getString(R.string.books_by_author), mBookAuthor.BookCount));
//        }
//      }
//
//      @Override
//      public void onClick(View view) {
//
//        Log.d(TAG, "++BookAuthorHolder::onClick(View)");
//        mCallback.onItemListAuthorSelected(mBookAuthor.AuthorName);
//      }
//    }
//
//    private final LayoutInflater mInflater;
//    private List<BookAuthor> mBookAuthorList;
//
//    BookAuthorAdapter(Context context) {
//
//      mInflater = LayoutInflater.from(context);
//    }
//
//    @NonNull
//    @Override
//    public BookAuthorHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//
//      View itemView = mInflater.inflate(R.layout.item_author, parent, false);
//      return new BookAuthorHolder(itemView);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull BookAuthorHolder holder, int position) {
//
//      if (mBookAuthorList != null) {
//        BookAuthor bookAuthor = mBookAuthorList.get(position);
//        holder.bind(bookAuthor);
//      } else {
//        // No books!
//      }
//    }
//
//    @Override
//    public int getItemCount() {
//
//      if (mBookAuthorList != null) {
//        return mBookAuthorList.size();
//      } else {
//        return 0;
//      }
//    }
//
//    void setBookAuthorList(List<BookAuthor> bookAuthorList) {
//
//      Log.d(TAG, "++setBookAuthorList(List<BookAuthor>)");
//      mBookAuthorList = bookAuthorList;
//      notifyDataSetChanged();
//    }
//  }
}

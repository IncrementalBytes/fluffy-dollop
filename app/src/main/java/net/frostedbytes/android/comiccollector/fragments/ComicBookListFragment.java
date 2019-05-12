package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ComicBookListFragment extends Fragment {

    private static final String TAG = BASE_TAG + ComicBookListFragment.class.getSimpleName();

    public interface OnComicBookListListener {

        void onComicListAddBook();

        void onComicListItemSelected(ComicBook comicBook);

        void onComicListPopulated(int size);

        void onComicListSynchronize();
    }

    private OnComicBookListListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<ComicBook> mComicBooks;

    public static ComicBookListFragment newInstance(ArrayList<ComicBook> comicBooks) {

        LogUtils.debug(TAG, "++newInstance(%d)", comicBooks.size());
        ComicBookListFragment fragment = new ComicBookListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(BaseActivity.ARG_COMIC_LIST, comicBooks);
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

        Bundle arguments = getArguments();
        if (arguments != null) {
            mComicBooks = arguments.getParcelableArrayList(BaseActivity.ARG_COMIC_LIST);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_comic_list, container, false);

        FloatingActionButton mAddButton = view.findViewById(R.id.comic_fab_add);
        mRecyclerView = view.findViewById(R.id.comic_list_view);
        FloatingActionButton mSyncButton = view.findViewById(R.id.comic_fab_sync);


        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);

        mAddButton.setOnClickListener(pickView -> mCallback.onComicListAddBook());
        mSyncButton.setOnClickListener(pickView -> mCallback.onComicListSynchronize());

        updateUI();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mComicBooks = null;
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mComicBooks == null || mComicBooks.size() == 0) {
            mCallback.onComicListPopulated(0);
        } else {
            LogUtils.debug(TAG, "++updateUI()");
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

        private final TextView mProductCodeTextView;
        private final ImageView mOwnImage;
        private final ImageView mWishListImage;

        private ComicBook mComicBook;

        ComicHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.comic_item, parent, false));

            mProductCodeTextView = itemView.findViewById(R.id.comic_item_text_product_code);
            mOwnImage = itemView.findViewById(R.id.comic_image_own);
            mWishListImage = itemView.findViewById(R.id.comic_image_wishlist);

            itemView.setOnClickListener(this);
        }

        void bind(ComicBook comicBook) {

            mComicBook = comicBook;

            mProductCodeTextView.setText(mComicBook.ProductCode);
            if (mComicBook.IsOwned) {
                mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_owned_light, null));
            } else {
                mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_not_owned_light, null));
            }

            mWishListImage.setVisibility(mComicBook.OnWishlist? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++ComicHolder::onClick(View)");
            mCallback.onComicListItemSelected(mComicBook);
        }
    }
}

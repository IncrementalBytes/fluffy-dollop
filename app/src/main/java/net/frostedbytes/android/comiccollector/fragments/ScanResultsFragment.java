package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class ScanResultsFragment extends Fragment {

    private static final String TAG = BASE_TAG + ScanResultsFragment.class.getSimpleName();

    public interface OnScanResultsListener {

        void onScanResultsPopulated(int size);

        void onScanResultsItemSelected(String searchText);
    }

    private OnScanResultsListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<String> mScanResults;

    public static ScanResultsFragment newInstance(ArrayList<String> scanResults) {

        LogUtils.debug(TAG, "++newInstance(ArrayList<>)");
        ScanResultsFragment fragment = new ScanResultsFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(BaseActivity.ARG_SCAN_RESULTS, scanResults);
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
            mCallback = (OnScanResultsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mScanResults = arguments.getStringArrayList(BaseActivity.ARG_SCAN_RESULTS);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_scan_results, container, false);

        mRecyclerView = view.findViewById(R.id.scan_list_view);

        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);

        updateUI();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mScanResults = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        LogUtils.debug(TAG, "++onResume()");
        //updateUI();
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mScanResults != null && mScanResults.size() > 0) {
            LogUtils.debug(TAG, "++updateUI()");
            ScanResultsAdapter scanResultsAdapter = new ScanResultsAdapter(mScanResults);
            mRecyclerView.setAdapter(scanResultsAdapter);
            mCallback.onScanResultsPopulated(scanResultsAdapter.getItemCount());
        } else {
            mCallback.onScanResultsPopulated(0);
        }
    }

    /**
     * Adapter class for MatchSummary objects
     */
    private class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsHolder> {

        private final List<String> mScanResults;

        ScanResultsAdapter(List<String> scanResults) {

            mScanResults = scanResults;
        }

        @NonNull
        @Override
        public ScanResultsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new ScanResultsHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ScanResultsHolder holder, int position) {

            String scanResult = mScanResults.get(position);
            holder.bind(scanResult);
        }

        @Override
        public int getItemCount() {
            return mScanResults.size();
        }
    }

    /**
     * Holder class for scan result objects
     */
    private class ScanResultsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mSearchTermTextView;

        private String mScanResult;

        ScanResultsHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.text_item, parent, false));

            itemView.setOnClickListener(this);
            mSearchTermTextView = itemView.findViewById(R.id.text_item_search_term);
        }

        void bind(String scanResult) {

            mScanResult = scanResult;
            mSearchTermTextView.setText(scanResult);
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++ScanResultsHolder::onClick(View)");
            mCallback.onScanResultsItemSelected(mScanResult);
        }
    }
}

package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class QueryFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryFragment.class.getSimpleName();

    public interface OnQueryListener {

        void onQueryActionComplete(String message);

        void onQueryShowManualInput();

        void onQueryTakePicture();
    }

    private OnQueryListener mCallback;

    public static QueryFragment newInstance() {

        LogUtils.debug(TAG, "++newInstance()");
        return new QueryFragment();
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtils.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnQueryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_comic_book_query, container, false);

        CardView scanPhotoCard = view.findViewById(R.id.query_card_photo);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanPhotoCard.setOnClickListener(v -> mCallback.onQueryTakePicture());
            } else {
                String message = "Camera feature is not available; disabling camera.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
                scanPhotoCard.setEnabled(false);
            }
        } else {
            String message = "Camera not detected.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
            scanPhotoCard.setEnabled(false);
        }

        CardView manualCard = view.findViewById(R.id.query_card_manual);
        manualCard.setOnClickListener(v -> mCallback.onQueryShowManualInput());

        mCallback.onQueryActionComplete("");
        return view;
    }
}

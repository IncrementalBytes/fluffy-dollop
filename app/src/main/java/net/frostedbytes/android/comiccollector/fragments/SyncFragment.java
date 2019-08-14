package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.User;

public class SyncFragment extends Fragment {

  private static final String TAG = BASE_TAG + "SyncFragment";

  public interface OnSyncListener {

    void onSyncExport();

    void onSyncFail();

    void onSyncImport();
  }

  private OnSyncListener mCallback;

  private User mUser;

  public static SyncFragment newInstance(User user) {

    LogUtils.debug(TAG, "++newInstance()");
    SyncFragment fragment = new SyncFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_USER, user);
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
      mCallback = (OnSyncListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUser = (User) arguments.getSerializable(BaseActivity.ARG_USER);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_sync, container, false);

    CardView exportCard = view.findViewById(R.id.sync_card_export);
    CardView importCard = view.findViewById(R.id.sync_card_import);

    if (mUser != null && User.isValid(mUser)) {
      exportCard.setOnClickListener(v -> mCallback.onSyncExport());
      importCard.setOnClickListener(v -> mCallback.onSyncImport());
    } else {
      mCallback.onSyncFail();
    }

    return view;
  }
}

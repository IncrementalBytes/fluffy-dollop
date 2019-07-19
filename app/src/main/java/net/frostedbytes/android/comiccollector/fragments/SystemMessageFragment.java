package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class SystemMessageFragment extends Fragment {

  private static final String TAG = BASE_TAG + "SystemMessageFragment";

  private String mMessage;

  public static SystemMessageFragment newInstance(String message) {

    SystemMessageFragment fragment = new SystemMessageFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_MESSAGE, message);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mMessage = arguments.getString(BaseActivity.ARG_MESSAGE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_system_message, container, false);
    TextView messageText = view.findViewById(R.id.system_text_message);
    messageText.setText(mMessage);
    return view;
  }
}

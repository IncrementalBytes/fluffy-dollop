package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
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

  public static SystemMessageFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new SystemMessageFragment();
  }

  public static SystemMessageFragment newInstance(String message) {

    LogUtils.debug(TAG, "++newInstance(%s)", message);
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
    ProgressBar progressBar = view.findViewById(R.id.system_progress);
    if (mMessage != null && mMessage.length() > 0) {
      messageText.setVisibility(View.VISIBLE);
      messageText.setText(mMessage);
      progressBar.setVisibility(View.INVISIBLE);
    } else {
      messageText.setVisibility(View.INVISIBLE);
      progressBar.setVisibility(View.VISIBLE);
      progressBar.setIndeterminate(true);
    }

    return view;
  }
}

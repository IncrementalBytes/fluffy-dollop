package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.User;

public class TutorialFragment extends Fragment {

  private static final String TAG = BASE_TAG + "TutorialFragment";

  public interface OnTutorialListener {

    void onTutorialContinue();

    void onTutorialShowHint(boolean show);
  }

  private OnTutorialListener mCallback;

  private User mUser;

  public static TutorialFragment newInstance(User user) {

    LogUtils.debug(TAG, "++newInstance()");
    TutorialFragment fragment = new TutorialFragment();
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
      mCallback = (OnTutorialListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUser = (User)arguments.getSerializable(BaseActivity.ARG_USER);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_tutorial, container, false);

    Switch showHintSwitch = view.findViewById(R.id.tutorial_switch_hide);
    showHintSwitch.setChecked(mUser.ShowBarcodeHint);
    showHintSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mCallback.onTutorialShowHint(isChecked));
    Button continueButton = view.findViewById(R.id.tutorial_button_continue);
    continueButton.setOnClickListener(v -> mCallback.onTutorialContinue());
    return view;
  }
}

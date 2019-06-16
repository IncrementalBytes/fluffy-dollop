package net.frostedbytes.android.comiccollector.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.frostedbytes.android.comiccollector.R;

public class InterludeFragment extends Fragment {

  public static InterludeFragment newInstance() {

    return new InterludeFragment();
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    return inflater.inflate(R.layout.fragment_interlude, container, false);
  }
}

package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class UserPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = BASE_TAG + UserPreferenceFragment.class.getSimpleName();

  public static final String IS_GEEK_PREFERENCE = "preference_is_geek";

  public interface OnPreferencesListener {

    void onPreferenceChanged();
  }

  private OnPreferencesListener mCallback;

  public static UserPreferenceFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new UserPreferenceFragment();
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnPreferencesListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    LogUtils.debug(TAG, "++onCreatePreferences(Bundle, String)");
    addPreferencesFromResource(R.xml.app_preferences);
  }

  @Override
  public void onPause() {
    super.onPause();

    LogUtils.debug(TAG, "++onPause()");
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();

    LogUtils.debug(TAG, "++onResume()");
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {

    LogUtils.debug(TAG, "++onSharedPreferenceChanged(SharedPreferences, String)");
    getPreferenceScreen().getSharedPreferences().edit().apply();
    mCallback.onPreferenceChanged();
  }
}

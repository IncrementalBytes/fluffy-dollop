/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.whollynugatory.android.comiccollector.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceFragmentCompat;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.BuildConfig;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.models.User;

public class UserPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = BaseActivity.BASE_TAG + "UserPreferenceFragment";

  public static final String IS_GEEK_PREFERENCE = "preference_is_geek";
  public static final String SHOW_TUTORIAL_PREFERENCE = "preference_show_tutorial";
  public static final String USE_IMAGE_PREVIEW_PREFERENCE = "preference_image_preview";
  public static final String FORCE_EXCPETION_PREFERENCE = "preference_force_exception";
  public static final String APP_VERSION_PREFERENCE = "preference_app_version";

  public interface OnPreferencesListener {

    void onPreferenceChanged() throws ComicCollectorException;
  }

  private OnPreferencesListener mCallback;

  private User mUser;

  public static UserPreferenceFragment newInstance(User user) {

    Log.d(TAG, "++newInstance()");
    UserPreferenceFragment fragment = new UserPreferenceFragment();
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

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnPreferencesListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUser = (User)arguments.getSerializable(BaseActivity.ARG_USER);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Log.d(TAG, "++onCreatePreferences(Bundle, String)");
    addPreferencesFromResource(R.xml.app_preferences);
    SwitchPreference switchPreference = (SwitchPreference) findPreference(IS_GEEK_PREFERENCE);
    if (switchPreference != null) {
      switchPreference.setChecked(mUser.IsGeek);
    }

    switchPreference = (SwitchPreference) findPreference(SHOW_TUTORIAL_PREFERENCE);
    if (switchPreference != null) {
      switchPreference.setChecked(mUser.ShowBarcodeHint);
    }

    switchPreference = (SwitchPreference) findPreference(USE_IMAGE_PREVIEW_PREFERENCE);
    if (switchPreference != null) {
      switchPreference.setChecked(mUser.UseImageCapture);
    }

    switchPreference = (SwitchPreference) findPreference(FORCE_EXCPETION_PREFERENCE);
    if (switchPreference != null) {
      if (BuildConfig.DEBUG) {
        switchPreference.setVisible(true);
      } else {
        switchPreference.setVisible(false);
      }
    }

    EditTextPreference textPreference = (EditTextPreference) findPreference(APP_VERSION_PREFERENCE);
    if (textPreference != null) {
      textPreference.setSummary(BuildConfig.VERSION_NAME);
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    Log.d(TAG, "++onPause()");
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {

    Log.d(TAG, "++onSharedPreferenceChanged(SharedPreferences, String)");
    getPreferenceScreen().getSharedPreferences().edit().apply();
    try {
      mCallback.onPreferenceChanged();
    } catch (ComicCollectorException e) {
      Log.d(TAG, "Exception!", e);
    }
  }
}

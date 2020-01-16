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
package net.whollynugatory.android.comiccollector.common;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import net.whollynugatory.android.comiccollector.BuildConfig;

import java.util.Locale;

/**
 * Wrapper class for logging to help remove from non-debug builds.
 */
public class LogUtils {

  public static void debug(final String tag, String message) {

    debug(tag, "%s", message);
  }

  public static void debug(final String tag, String message, Exception e) {

    if (e != null) {
      Log.d(tag, message, e);
    } else {
      Log.d(tag, message);
    }
  }

  public static void debug(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.d(tag, String.format(Locale.US, messageFormat, args));
    } else {
      Crashlytics.log(Log.DEBUG, tag, String.format(Locale.US, messageFormat, args));
    }
  }

  public static void error(final String tag, String message) {

    error(tag, "%s", message);
  }

  public static void error(final String tag, String message, Exception e) {

    if (BuildConfig.DEBUG) {
      if (e != null) {
        Log.e(tag, message, e);
      } else {
        Log.e(tag, message);
      }
    } else {
      Crashlytics.log(Log.ERROR, tag, message);
      if (e != null) {
        Crashlytics.logException(e);
      }
    }
  }

  public static void error(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.e(tag, String.format(Locale.US, messageFormat, args));
    } else {
      Crashlytics.log(Log.ERROR, tag, String.format(Locale.US, messageFormat, args));
    }
  }

  public static void warn(final String tag, String message) {

    warn(tag, "%s", message);
  }

  public static void warn(final String tag, String message, Exception e) {

    if (BuildConfig.DEBUG) {
      if (e != null) {
        Log.w(tag, message, e);
      } else {
        Log.w(tag, message);
      }
    } else {
      Crashlytics.log(Log.WARN, tag, message);
      if (e != null) {
        Crashlytics.logException(e);
      }
    }
  }

  public static void warn(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.w(tag, String.format(Locale.US, messageFormat, args));
    } else {
      Crashlytics.log(Log.WARN, tag, String.format(Locale.US, messageFormat, args));
    }
  }
}

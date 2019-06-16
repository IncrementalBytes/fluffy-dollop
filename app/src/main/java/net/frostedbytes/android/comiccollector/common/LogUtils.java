package net.frostedbytes.android.comiccollector.common;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import net.frostedbytes.android.comiccollector.BuildConfig;

import java.util.Locale;

/**
 * Wrapper class for logging to help remove from non-debug builds.
 */
public class LogUtils {

  public static void debug(final String tag, String message) {

    debug(tag, "%s", message);
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

  public static void warn(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.w(tag, String.format(Locale.US, messageFormat, args));
    } else {
      Crashlytics.log(Log.WARN, tag, String.format(Locale.US, messageFormat, args));
    }
  }
}

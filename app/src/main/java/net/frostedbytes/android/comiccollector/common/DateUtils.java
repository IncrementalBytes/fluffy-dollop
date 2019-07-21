package net.frostedbytes.android.comiccollector.common;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

  public static String formatDateForDisplay(long date) {

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(date);
    return String.format(Locale.US, "%02d/%04d", calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
  }
}

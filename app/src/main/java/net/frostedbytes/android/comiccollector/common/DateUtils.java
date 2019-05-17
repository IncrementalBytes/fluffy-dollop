package net.frostedbytes.android.comiccollector.common;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static String formatDateForDisplay(long date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        return String.format(Locale.US, "%d/%d", calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
    }

    public static long fromString(String date) {

        int month;
        int day = 1;
        int year;
        String[] segments = date.split("/");
        if (segments.length == 2) { // month and year
            month = Integer.parseInt(segments[0]);
            year = Integer.parseInt(segments[1]);
        } else if (segments.length == 3){ // month, day, and year
            month = Integer.parseInt(segments[0]);
            day = Integer.parseInt(segments[1]);
            year = Integer.parseInt(segments[2]);
        } else {
            month = Calendar.getInstance().get(Calendar.MONTH);
            year = Calendar.getInstance().get(Calendar.YEAR);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.YEAR, year);
        return calendar.getTimeInMillis();
    }
}

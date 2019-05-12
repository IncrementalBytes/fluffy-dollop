package net.frostedbytes.android.comiccollector.common;

import java.util.Locale;

public class PathUtils {

    public static String combine(Object... paths) {

        String finalPath = "";
        for (Object path : paths) {
            String format = "%s/%s";
            if (path.getClass() == Integer.class) {
                format = "%s/%d";
            }

            finalPath = String.format(Locale.US, format, finalPath, path);
        }

        return finalPath;
    }
}

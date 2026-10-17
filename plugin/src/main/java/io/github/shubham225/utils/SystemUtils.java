package io.github.shubham225.utils;

import java.util.Locale;

/** System Utility functions */
public class SystemUtils {
    /**
     * Checks if running on Windows os
     *
     * @return true if Windows os
     */
    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    /**
     * Checks if running on older version of Windows os
     *
     * @return true if Windows os
     */
    public static boolean isWindows10OrNewer() {
        String[] v = System.getProperty("os.version", "0").split("\\.");
        try {
            return Integer.parseInt(v[0]) >= 10;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

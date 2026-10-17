package io.github.shubham225.utils;

import java.util.Locale;

/** Format values. */
public class Formatter {

    /**
     * Under a minute: "12.345 s". A minute or more: "2m 29s". An hour or more: "1h 2m 5s".
     *
     * @param ms time in milliseconds
     * @return formatted string of time
     */
    public static String formatTime(long ms) {
        if (ms < 60_000) {
            return String.format(Locale.ROOT, "%.3f s", ms / 1000.0);
        }
        long totalSeconds = Math.round(ms / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? hours + "h " + minutes + "m " + seconds + "s"
                : minutes + "m " + seconds + "s";
    }
}

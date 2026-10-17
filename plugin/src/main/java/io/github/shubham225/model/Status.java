package io.github.shubham225.model;

/** Status values. */
public class Status {
    /** Executed module count. */
    public int executed = 0;

    /** Upto Date module count. */
    public int upToDate = 0;

    /** Skipped module count. */
    public int skipped = 0;

    /** Failed module count. */
    public int failed = 0;

    /** Start Time. */
    public long firstStart = Long.MAX_VALUE;

    /** End Time. */
    public long lastEnd = 0;

    /**
     * returns status of execution
     *
     * @return string value of status
     */
    public String status() {
        if (failed > 0) return "FAILED";
        if (executed > 0) return "SUCCESS";
        if (upToDate > 0) return "UP-TO-DATE";
        return "SKIPPED";
    }

    /**
     * returns execution time in ms
     *
     * @return time in milliseconds
     */
    public long millis() {
        return Math.max(0, lastEnd - firstStart);
    }
}

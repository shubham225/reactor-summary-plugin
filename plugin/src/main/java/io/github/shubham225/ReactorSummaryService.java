package io.github.shubham225;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskOperationDescriptor;
import org.gradle.tooling.events.task.TaskOperationResult;
import org.gradle.tooling.events.task.TaskSkippedResult;
import org.gradle.tooling.events.task.TaskSuccessResult;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Build service that collects task results from every module and prints the
 * Reactor Summary when the build finishes.
 */
public abstract class ReactorSummaryService
        implements BuildService<ReactorSummaryService.Params>,
        OperationCompletionListener,
        AutoCloseable {

    /** Parameters passed from the plugin to the service. */
    public interface Params extends BuildServiceParameters {

        /**
         * Whether the summary should use ANSI colors.
         *
         * @return the color flag
         */
        Property<Boolean> getColor();

        /**
         * Name of the root project, shown for the root module.
         *
         * @return the root project name
         */
        Property<String> getRootName();

        /**
         * Delay in milliseconds before the summary is printed.
         *
         * @return the delay in milliseconds
         */
        Property<Long> getDelayMillis();
    }

    private static final Logger LOGGER = Logging.getLogger("reactor-summary");

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final int NAME_WIDTH = 50;
    private static final int STATUS_WIDTH = 10;

    private static class Stat {
        int executed, upToDate, skipped, failed;
        long firstStart = Long.MAX_VALUE, lastEnd = 0;

        String status() {
            if (failed > 0) return "FAILED";
            if (executed > 0) return "SUCCESS";
            if (upToDate > 0) return "UP-TO-DATE";
            return "SKIPPED";
        }

        long millis() {
            return Math.max(0, lastEnd - firstStart);
        }
    }

    private final Map<String, Stat> modules = new TreeMap<>();
    private final long buildStart = System.currentTimeMillis();
    private final AtomicBoolean printed = new AtomicBoolean(false);

    // Events

    @Override
    public synchronized void onFinish(FinishEvent event) {
        if (!(event instanceof TaskFinishEvent)) return;
        TaskFinishEvent e = (TaskFinishEvent) event;

        String taskPath = ((TaskOperationDescriptor) e.getDescriptor()).getTaskPath();
        int idx = taskPath.lastIndexOf(':');
        String module = idx <= 0 ? ":" : taskPath.substring(0, idx);

        TaskOperationResult r = e.getResult();
        Stat s = modules.computeIfAbsent(module, k -> new Stat());
        s.firstStart = Math.min(s.firstStart, r.getStartTime());
        s.lastEnd = Math.max(s.lastEnd, r.getEndTime());

        if (r instanceof TaskFailureResult) {
            s.failed++;
        } else if (r instanceof TaskSkippedResult) {
            s.skipped++;
        } else if (r instanceof TaskSuccessResult) {
            TaskSuccessResult ok = (TaskSuccessResult) r;
            if (ok.isUpToDate() || ok.isFromCache()) s.upToDate++; else s.executed++;
        }
    }

    // Printing

    /** Called by Gradle when the build finishes. Prints after a short delay. */
    @Override
    public void close() {
        printLater(getParameters().getDelayMillis().getOrElse(10L));
    }

    private synchronized void printLater(long delay) {
        if (modules.isEmpty() || !printed.compareAndSet(false, true)) return;

        String text = render(getParameters().getColor().getOrElse(false));

        if (delay <= 0) {
            LOGGER.lifecycle(text);
            return;
        }

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return;
            }
            LOGGER.lifecycle(text);
        }, "reactor-summary-printer");
        t.setDaemon(true);
        t.start();
    }

    private String render(boolean color) {
        String rootName = getParameters().getRootName().getOrElse("root");
        String line = paint(color, repeat("-", 70), DIM);

        int success = 0, upToDate = 0, skipped = 0, failed = 0;

        StringBuilder sb = new StringBuilder("\n");
        sb.append(line).append('\n')
                .append(paint(color, "Reactor Summary", BOLD)).append('\n')
                .append(line).append('\n');

        for (Map.Entry<String, Stat> en : modules.entrySet()) {
            Stat s = en.getValue();
            String status = s.status();
            switch (status) {
                case "SUCCESS":    success++;  break;
                case "UP-TO-DATE": upToDate++; break;
                case "SKIPPED":    skipped++;  break;
                default:           failed++;
            }

            String name = en.getKey().equals(":") ? rootName + " (root)" : en.getKey().substring(1);
            int dots = Math.max(2, NAME_WIDTH - name.length());

            sb.append(name).append(' ')
                    .append(paint(color, repeat(".", dots), DIM)).append(' ')
                    .append(paint(color, pad(status, STATUS_WIDTH), statusColor(status)))
                    .append(' ')
                    .append(paint(color, "[" + formatTime(s.millis()) + "]", DIM))
                    .append('\n');
        }

        sb.append(line).append('\n')
                .append(paint(color, "Modules: " + modules.size(), BOLD)).append("  ")
                .append(paint(color, success + " success", GREEN)).append("  ")
                .append(paint(color, upToDate + " up-to-date", CYAN)).append("  ")
                .append(paint(color, skipped + " skipped", YELLOW)).append("  ")
                .append(paint(color, failed + " failed", failed > 0 ? RED + BOLD : DIM)).append('\n')
                .append("Total time: ")
                .append(paint(color, formatTime(System.currentTimeMillis() - buildStart), BOLD)).append('\n')
                .append(line);
        return sb.toString();
    }

    private static String statusColor(String status) {
        switch (status) {
            case "SUCCESS":    return GREEN + BOLD;
            case "UP-TO-DATE": return CYAN;
            case "SKIPPED":    return YELLOW;
            default:           return RED + BOLD;
        }
    }

    private static String paint(boolean color, String text, String codes) {
        return color ? codes + text + RESET : text;
    }

    private static String pad(String s, int width) {
        return String.format("%-" + width + "s", s);
    }

    private static String formatTime(long ms) {
        return String.format("%.3f s", ms / 1000.0);
    }

    private static String repeat(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }
}
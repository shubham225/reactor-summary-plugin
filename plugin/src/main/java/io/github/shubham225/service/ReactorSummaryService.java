package io.github.shubham225.service;

import io.github.shubham225.constants.AnsiColor;
import io.github.shubham225.constants.AnsiStyle;
import io.github.shubham225.constants.PluginConstant;
import io.github.shubham225.model.Status;
import io.github.shubham225.utils.Formatter;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskOperationResult;
import org.gradle.tooling.events.task.TaskSkippedResult;
import org.gradle.tooling.events.task.TaskSuccessResult;

import java.util.Collections;
import java.util.Locale;
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

    private final Map<String, Status> modules = new TreeMap<>();
    private final long buildStart = System.currentTimeMillis();
    private final AtomicBoolean printed = new AtomicBoolean(false);

    // Events
    @Override
    public synchronized void onFinish(FinishEvent event) {
        if (!(event instanceof TaskFinishEvent)) return;
        TaskFinishEvent e = (TaskFinishEvent) event;

        String taskPath = e.getDescriptor().getTaskPath();
        int idx = taskPath.lastIndexOf(':');
        String module = idx <= 0 ? ":" : taskPath.substring(0, idx);

        TaskOperationResult r = e.getResult();
        Status s = modules.computeIfAbsent(module, k -> new Status());
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
        printLater(getParameters().getDelayMillis().getOrElse(PluginConstant.DEFAULT_DELAY_MILLIS));
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
        String line = paint(color, repeat("-", 70), AnsiStyle.DIM);

        int success = 0, upToDate = 0, skipped = 0, failed = 0;

        StringBuilder sb = new StringBuilder("\n");
        sb.append(line).append('\n')
                .append(paint(color, "Reactor Summary", AnsiStyle.BOLD)).append('\n')
                .append(line).append('\n');

        for (Map.Entry<String, Status> en : modules.entrySet()) {
            Status s = en.getValue();
            String status = s.status();
            switch (status) {
                case "SUCCESS":    success++;  break;
                case "UP-TO-DATE": upToDate++; break;
                case "SKIPPED":    skipped++;  break;
                default:           failed++;
            }

            String name = en.getKey().equals(":") ? rootName + " (root)" : en.getKey().substring(1);
            int dots = Math.max(2, PluginConstant.NAME_WIDTH - name.length());

            sb.append(name).append(' ')
                    .append(paint(color, repeat(".", dots), AnsiStyle.DIM)).append(' ')
                    .append(paint(color, pad(status, PluginConstant.STATUS_WIDTH), statusColor(status)))
                    .append(' ')
                    .append(paint(color, "[" + Formatter.formatTime(s.millis()) + "]", AnsiStyle.DIM))
                    .append('\n');
        }

        sb.append(line).append('\n')
                .append(paint(color, "Modules: " + modules.size(), AnsiStyle.BOLD)).append("  ")
                .append(paint(color, success + " success", AnsiColor.GREEN)).append("  ")
                .append(paint(color, upToDate + " up-to-date", AnsiColor.CYAN)).append("  ")
                .append(paint(color, skipped + " skipped", AnsiColor.YELLOW)).append("  ")
                .append(paint(color, failed + " failed", failed > 0 ? AnsiColor.RED + AnsiStyle.BOLD : AnsiStyle.DIM)).append('\n')
                .append("Total time: ")
                .append(paint(color, Formatter.formatTime(System.currentTimeMillis() - buildStart), AnsiStyle.BOLD)).append('\n')
                .append(line);
        return sb.toString();
    }

    private String statusColor(String status) {
        switch (status) {
            case "SUCCESS":    return AnsiColor.GREEN + AnsiStyle.BOLD;
            case "UP-TO-DATE": return AnsiColor.CYAN;
            case "SKIPPED":    return AnsiColor.YELLOW;
            default:           return AnsiColor.RED + AnsiStyle.BOLD;
        }
    }

    private String paint(boolean color, String text, String codes) {
        return color ? codes + text + AnsiStyle.DEFAULT : text;
    }

    private String pad(String s, int width) {
        return String.format("%-" + width + "s", s);
    }

    private String repeat(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }
}
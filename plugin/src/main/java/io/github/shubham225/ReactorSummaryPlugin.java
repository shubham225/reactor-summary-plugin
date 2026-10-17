package io.github.shubham225;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

/**
 * Gradle plugin that prints a Maven-style "Reactor Summary" (status and elapsed
 * time per module) at the end of a multi-module build.
 *
 * <p>Apply it once, on the root project.
 */
public class ReactorSummaryPlugin implements Plugin<Project> {

    private final BuildEventsListenerRegistry registry;

    /**
     * Creates the plugin.
     *
     * @param registry Gradle's registry used to receive task-completion events
     */
    @Inject
    public ReactorSummaryPlugin(BuildEventsListenerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            return; // apply once, on the root project only
        }

        boolean color = resolveColor(project);
        String rootName = project.getName();
        long delay = resolveDelay(project);

        Provider<ReactorSummaryService> service = project.getGradle()
                .getSharedServices()
                .registerIfAbsent("reactorSummary", ReactorSummaryService.class, spec -> {
                    spec.getParameters().getColor().set(color);
                    spec.getParameters().getRootName().set(rootName);
                    spec.getParameters().getDelayMillis().set(delay);
                });
        registry.onTaskCompletion(service);
    }

    /** -PreactorSummary.color=true|false (or in gradle.properties). */
    private static boolean resolveColor(Project project) {
        Object prop = project.findProperty("reactorSummary.color");
        if (prop != null) {
            String v = prop.toString().trim();
            if (v.equalsIgnoreCase("true")) return true;
            if (v.equalsIgnoreCase("false")) return false;
        }
        ConsoleOutput console = project.getGradle().getStartParameter().getConsoleOutput();
        switch (console) {
            case Plain:
                return false;
            case Rich:
            case Verbose:
                return true;
            default: // Auto
                return autoDetectColor();
        }
    }

    /**
     * Best-effort ANSI detection for {@code --console=auto}.
     *
     * <p>The build runs in the Gradle daemon, which has no terminal of its own,
     * so we can only look at the environment forwarded from the client. On
     * Linux/macOS (and Git Bash/MSYS on Windows) {@code TERM} is set, but
     * cmd.exe and PowerShell never set it, so Windows needs its own checks.
     */
    private static boolean autoDetectColor() {
        Map<String, String> env = System.getenv();

        String noColor = env.get("NO_COLOR");
        if (noColor != null && !noColor.isEmpty()) return false;   // https://no-color.org
        if (env.get("CI") != null) return false;                    // logs, not terminals

        String term = env.get("TERM");
        if ("dumb".equals(term)) return false;
        if (term != null || env.get("COLORTERM") != null) return true;

        if (isWindows()) {
            return env.get("WT_SESSION") != null                    // Windows Terminal
                    || env.get("TERM_PROGRAM") != null              // VS Code, etc.
                    || env.get("ANSICON") != null                   // ANSICON
                    || "ON".equalsIgnoreCase(env.get("ConEmuANSI")) // ConEmu / Cmder
                    || env.get("IDEA_INITIAL_DIRECTORY") != null    // IntelliJ terminal
                    || isWindows10OrNewer();                        // conhost supports VT
        }
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static boolean isWindows10OrNewer() {
        String[] v = System.getProperty("os.version", "0").split("\\.");
        try {
            return Integer.parseInt(v[0]) >= 10;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** -PreactorSummary.delayMs=30 ; 0 prints immediately (above Gradle's BUILD line). */
    private static long resolveDelay(Project project) {
        Object prop = project.findProperty("reactorSummary.delayMs");
        if (prop != null) {
            try {
                return Long.parseLong(prop.toString().trim());
            } catch (NumberFormatException ignored) {
                // use default
            }
        }
        return 30L;
    }
}
package io.github.shubham225;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;

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
                String term = System.getenv("TERM");
                return System.getenv("NO_COLOR") == null
                        && term != null && !term.equals("dumb");
        }
    }

    /** -PreactorSummary.delayMs=1000 ; 0 prints immediately (above Gradle's BUILD line). */
    private static long resolveDelay(Project project) {
        Object prop = project.findProperty("reactorSummary.delayMs");
        if (prop != null) {
            try {
                return Long.parseLong(prop.toString().trim());
            } catch (NumberFormatException ignored) {
                // use default
            }
        }
        return 10L;
    }
}
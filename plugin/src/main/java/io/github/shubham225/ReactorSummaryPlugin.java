package io.github.shubham225;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;

public class ReactorSummaryPlugin implements Plugin<Project> {

    private final BuildEventsListenerRegistry registry;
    private final FlowScope flowScope;
    private final FlowProviders flowProviders;

    @Inject
    public ReactorSummaryPlugin(BuildEventsListenerRegistry registry,
                                FlowScope flowScope,
                                FlowProviders flowProviders) {
        this.registry = registry;
        this.flowScope = flowScope;
        this.flowProviders = flowProviders;
    }

    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            return; // apply once, on the root project only
        }

        boolean color = resolveColor(project);
        String rootName = project.getName();

        // Collects task results from every module
        Provider<ReactorSummaryService> service = project.getGradle()
                .getSharedServices()
                .registerIfAbsent("reactorSummary", ReactorSummaryService.class, spec -> {
                    spec.getParameters().getColor().set(color);
                    spec.getParameters().getRootName().set(rootName);
                });
        registry.onTaskCompletion(service);

        // Prints the summary when the build finishes
        flowScope.always(ReactorSummaryFlowAction.class, spec ->
                spec.getParameters().getBuildResult().set(flowProviders.getBuildWorkResult()));
    }

    /** Override with -PreactorSummary.color=true|false (or in gradle.properties). */
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
}
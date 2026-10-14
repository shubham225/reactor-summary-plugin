package io.github.shubham225;

import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.Input;

public abstract class ReactorSummaryFlowAction
        implements FlowAction<ReactorSummaryFlowAction.Params> {

    public interface Params extends FlowParameters {
        // Resolves to the shared service registered under this name in the plugin
        @ServiceReference("reactorSummary")
        Property<ReactorSummaryService> getService();

        // Not read; it makes Gradle run this action at the end of the build
        @Input
        Property<BuildWorkResult> getBuildResult();
    }

    @Override
    public void execute(Params parameters) {
        parameters.getService().get().printSummary();
    }
}
package org.exbio.pipejar.pipeline;

import org.exbio.pipejar.configs.ConfigModuleCollection;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;

import java.util.Collection;

public abstract class ExecutableStepWithoutConfigs extends ExecutableStep<ConfigModuleCollection> {

    protected ExecutableStepWithoutConfigs(boolean add, OutputFile... dependencies) {
        super(null, add, dependencies);
    }

    public ExecutableStepWithoutConfigs() {
        super(null);
    }

    protected ExecutableStepWithoutConfigs(boolean add, Collection<OutputFile> dependencies) {
        super(null, add, dependencies);
    }

    public ExecutableStepWithoutConfigs(boolean add, Collection<OutputFile>[] dependencies) {
        super(null, add, dependencies);
    }

    protected ExecutableStepWithoutConfigs(boolean add, Collection<OutputFile> dependencies,
                                           OutputFile... otherDependencies) {
        super(null, add, dependencies, otherDependencies);
    }
}

package org.exbio.pipejar.configs.ConfigTypes.UsageTypes;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class RequiredConfig<T> extends UsageConfig<T> {
    public RequiredConfig(InputConfig<T> input) {
        super(input);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}

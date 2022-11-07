package org.exbio.pipejar.configs.ConfigTypes.UsageTypes;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class OptionalConfig<T> extends UsageConfig<T> {
    private final boolean isRequired;

    public OptionalConfig(InputConfig<T> input, boolean isRequired) {
        super(input);
        this.isRequired = isRequired;
    }


    @Override
    public boolean isRequired() {
        return isRequired;
    }
}

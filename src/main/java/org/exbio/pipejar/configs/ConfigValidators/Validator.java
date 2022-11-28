package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public abstract class Validator<T> {
    public abstract boolean validate(InputConfig<T> config);

    public abstract String toString();
}

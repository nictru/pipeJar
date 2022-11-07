package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public interface Validator<T> {
    boolean validate(InputConfig<T> config);

    String toString();
}

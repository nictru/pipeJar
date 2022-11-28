package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

import java.util.Collection;
import java.util.List;

public abstract class MultiValidator<T> extends Validator<T> {
    public abstract boolean validate(Collection<InputConfig<T>> configs);

    @SafeVarargs
    public final boolean validate(InputConfig<T>... configs) {
        return validate(List.of(configs));
    }

    @Override
    public boolean validate(InputConfig<T> config) {
        return validate(List.of(config));
    }
}

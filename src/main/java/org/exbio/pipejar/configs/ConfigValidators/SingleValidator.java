package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public abstract class SingleValidator<T> extends Validator<T> {
    public abstract boolean validateSingle(InputConfig<T> config);
    @SafeVarargs
    @Override
    public final boolean validate(InputConfig<T>... configs) {
        if(configs.length != 1){
            throw new IllegalArgumentException("SingleValidator can only validate one config");
        }
        return validateSingle(configs[0]);
    }
}

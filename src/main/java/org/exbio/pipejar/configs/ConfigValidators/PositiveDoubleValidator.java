package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class PositiveDoubleValidator implements Validator<Double> {
    @Override
    public boolean validate(InputConfig<Double> config) {
        if (config.isSet()) {
            return config.get() > 0;
        }
        return true;
    }
}

package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class PositiveDoubleValidator extends SingleValidator<Double> {
    @Override
    public boolean validateSingle(InputConfig<Double> config) {
        if (config.isSet()) {
            return config.get() > 0;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Only values greater than 0 are allowed.";
    }
}

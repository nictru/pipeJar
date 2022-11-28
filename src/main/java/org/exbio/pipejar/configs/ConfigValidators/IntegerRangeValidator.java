package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class IntegerRangeValidator extends Validator<Integer> {
    private final int min, max;

    public IntegerRangeValidator(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(InputConfig<Integer> config) {
        if (config.get() == null) {
            return true;
        }
        return min <= config.get() && config.get() <= max;
    }

    @Override
    public String toString() {
        return "Value has to be between " + min + " and " + max + " (borders included)";
    }
}

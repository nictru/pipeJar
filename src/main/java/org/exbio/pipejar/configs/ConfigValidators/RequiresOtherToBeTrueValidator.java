package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class RequiresOtherToBeTrueValidator extends Validator<Boolean> {
    private final InputConfig<Boolean> other;

    public RequiresOtherToBeTrueValidator(InputConfig<Boolean> other) {
        this.other = other;
    }


    @Override
    public boolean validate(InputConfig<Boolean> config) {
        return (config.isSet() && config.get()) ? other.get() : true;
    }

    @Override
    public String toString() {
        return "Requires " + other.getName() + " to be true.";
    }
}

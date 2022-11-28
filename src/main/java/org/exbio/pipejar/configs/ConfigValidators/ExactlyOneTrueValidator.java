package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

import java.util.Collection;

public class ExactlyOneTrueValidator extends MultiValidator<Boolean> {
    @Override
    public boolean validate(Collection<InputConfig<Boolean>> inputConfigs) {
        return inputConfigs.stream().filter(InputConfig::get).count() == 1;
    }

    @Override
    public String toString() {
        return "Exactly one of the associated configs hast to be true.";
    }
}

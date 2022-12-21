package org.exbio.pipejar.configs.ConfigValidators;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

import java.util.Arrays;

public class ExactlyOneTrueValidator extends  Validator<Boolean> {
    @SafeVarargs
    @Override
    public final boolean validate(InputConfig<Boolean>... configs) {
        return Arrays.stream(configs).filter(InputConfig::get).count() == 1;
    }

    @Override
    public String toString() {
        return "Exactly one of the options must be true";
    }
}

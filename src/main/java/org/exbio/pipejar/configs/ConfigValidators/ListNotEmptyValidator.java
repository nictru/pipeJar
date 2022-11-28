package org.exbio.pipejar.configs.ConfigValidators;

import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

import java.util.List;

public class ListNotEmptyValidator<T> extends Validator<List<T>> {
    @Override
    public boolean validate(InputConfig<List<T>> config) {
        if (config.get() == null) {
            return true;
        }
        return !config.get().isEmpty();
    }

    @Override
    public String toString() {
        return "Empty list is not allowed. Set to null instead.";
    }
}

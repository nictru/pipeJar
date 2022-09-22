package configs.ConfigValidators;

import configs.ConfigTypes.InputTypes.InputConfig;

public interface Validator<T> {
    boolean validate(InputConfig<T> config);

    String toString();
}

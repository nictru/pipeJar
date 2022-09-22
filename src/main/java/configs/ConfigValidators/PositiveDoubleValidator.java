package configs.ConfigValidators;

import configs.ConfigTypes.InputTypes.InputConfig;

public class PositiveDoubleValidator implements Validator<Double> {
    @Override
    public boolean validate(InputConfig<Double> config) {
        if (config.isSet()) {
            return config.get() > 0;
        }
        return true;
    }
}

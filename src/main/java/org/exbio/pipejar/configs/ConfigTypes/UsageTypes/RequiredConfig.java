package configs.ConfigTypes.UsageTypes;

import configs.ConfigTypes.InputTypes.InputConfig;

public class RequiredConfig<T> extends UsageConfig<T> {
    public RequiredConfig(InputConfig<T> input) {
        super(input);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}

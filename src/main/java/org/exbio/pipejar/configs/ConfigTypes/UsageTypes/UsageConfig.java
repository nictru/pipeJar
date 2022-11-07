package configs.ConfigTypes.UsageTypes;

import configs.ConfigTypes.Config;
import configs.ConfigTypes.InputTypes.InputConfig;
import org.apache.logging.log4j.Logger;

public abstract class UsageConfig<T> extends Config<T> {
    private final InputConfig<T> inputConfig;

    public UsageConfig(InputConfig<T> input) {
        inputConfig = input;
    }

    @Override
    public boolean isValid(Logger logger) {
        return inputConfig.isValid(logger);
    }

    @Override
    public boolean isSet() {
        return inputConfig.isSet();
    }

    @Override
    public String getName() {
        return inputConfig.getName();
    }

    @Override
    public T get() {
        return inputConfig.get();
    }

    public abstract boolean isRequired();
}

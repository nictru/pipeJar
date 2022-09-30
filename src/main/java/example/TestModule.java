package example;

import configs.ConfigModule;
import configs.ConfigTypes.InputTypes.ExternalConfig;
import configs.ConfigTypes.InputTypes.InputConfig;
import configs.ConfigTypes.InputTypes.InternalConfig;

public class TestModule extends ConfigModule {
    public final InputConfig<Integer> testConfig = new InternalConfig<>(10);
    public final InputConfig<Integer> optionalConfig = new ExternalConfig<>(Integer.class);
    public final InputConfig<Boolean> secondConfig = new ExternalConfig<>(Boolean.class);
}

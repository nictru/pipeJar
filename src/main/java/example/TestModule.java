package example;

import configs.ConfigModule;
import configs.ConfigTypes.InputTypes.InputConfig;
import configs.ConfigTypes.InputTypes.InternalConfig;
import configs.ConfigTypes.InputTypes.SettableConfig;

public class TestModule extends ConfigModule {
    public final InputConfig<Integer> testConfig = new InternalConfig<>(10);
    public final InputConfig<Integer> optionalConfig = new SettableConfig<Integer>(5);
}

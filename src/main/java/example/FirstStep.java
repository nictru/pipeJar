package example;

import configs.ConfigTypes.UsageTypes.OptionalConfig;
import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class FirstStep extends ExecutableStep {
    public RequiredConfig<Integer> test = new RequiredConfig<>(Main.ConfigModules.testModule.testConfig);
    public OptionalConfig<Integer> optional = new OptionalConfig<>(Main.ConfigModules.testModule.optionalConfig, test.get() > 3);

    @Override
    protected Set<BooleanSupplier> getSuppliers() {
        return new HashSet<>() {{
            for (int i = 0; i < test.get(); i++) {
                int finalI = i + optional.get();
                add(() -> {
                    logger.info("Task " + finalI);
                    return true;
                });
            }
        }};
    }
}

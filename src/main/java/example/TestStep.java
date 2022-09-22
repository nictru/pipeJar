package example;

import configs.ConfigTypes.UsageTypes.OptionalConfig;
import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

public class TestStep extends ExecutableStep {
    public RequiredConfig<Integer> test = new RequiredConfig<>(Main.ConfigModules.testModule.testConfig);
    public OptionalConfig<Integer> optional = new OptionalConfig<>(Main.ConfigModules.testModule.optionalConfig, test.get() > 3);

    @Override
    protected void execute() {
        for (int i = 0; i < test.get(); i++) {
            int finalI = i + optional.get();
            executorService.submit(() -> logger.info("Task " + finalI));
        }
    }
}

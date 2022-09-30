package example;

import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class SecondStep extends ExecutableStep {
    public RequiredConfig<Boolean> req = new RequiredConfig<>(Main.ConfigModules.testModule.secondConfig);

    public SecondStep(ExecutableStep... dependencies) {
        super(dependencies);
    }

    @Override
    protected Set<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                System.out.println("Test");
                return true;
            });
        }};
    }
}

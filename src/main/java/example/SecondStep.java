package example;

import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class SecondStep extends ExecutableStep {
    public RequiredConfig<Boolean> req = new RequiredConfig<>(Main.ConfigModules.testModule.secondConfig);

    public SecondStep(ExecutableStep first) {
        super(first);
    }

    @Override
    protected Set<BooleanSupplier> getSuppliers() {
        return new HashSet<>() {{
            add(() -> {
                System.out.println("Test");
                return true;
            });
        }};
    }
}

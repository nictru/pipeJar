package example;

import configs.ConfigTypes.FileTypes.InputFile;
import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class SecondStep extends ExecutableStep {
    private final InputFile firstFile;
    public RequiredConfig<Boolean> req = new RequiredConfig<>(Main.ConfigModules.testModule.secondConfig);

    public SecondStep(FirstStep first) {
        super(first);
        this.firstFile = input(first.created);
        updateOutputFiles();
    }

    @Override
    protected Set<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(firstFile))) {
                    reader.lines().forEachOrdered(logger::info);
                }
                return true;
            });
        }};
    }
}

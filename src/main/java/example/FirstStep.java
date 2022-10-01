package example;

import configs.ConfigTypes.FileTypes.OutputFile;
import configs.ConfigTypes.UsageTypes.OptionalConfig;
import configs.ConfigTypes.UsageTypes.RequiredConfig;
import pipeline.ExecutableStep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class FirstStep extends ExecutableStep {
    public final RequiredConfig<Integer> test = new RequiredConfig<>(Main.ConfigModules.testModule.testConfig);
    public final OptionalConfig<Integer> optional = new OptionalConfig<>(Main.ConfigModules.testModule.optionalConfig, test.get() > 3);

    public OutputFile created = new OutputFile("output.txt");

    public FirstStep() {
        super();
        updateOutputFiles();
    }

    @Override
    protected Set<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(created))) {
                    for (int i = 0; i < test.get(); i++) {
                        writer.write(String.valueOf(i + optional.get()));
                        writer.newLine();
                    }
                }
                return true;
            });
        }};
    }
}

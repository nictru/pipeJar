package example;

import configs.ConfigModuleCollection;
import pipeline.ExecutableStep;

import java.io.File;
import java.io.IOException;

import static pipeline.ExecutionManager.*;

public class Main {
    public static void main(String[] args) throws IOException {
        ConfigModules modules = new ConfigModules();

        modules.merge(new File("/home/nico/Software/pipeJar/src/main/resources/configs.json"));

        ExecutableStep first = new FirstStep();

        ExecutableStep second = new SecondStep(first);

        addSteps(first, second);

        if (simulate()) {
            execute();
        }
        shutdown();
    }

    protected static class ConfigModules extends ConfigModuleCollection {
        public static final TestModule testModule = new TestModule();
    }
}

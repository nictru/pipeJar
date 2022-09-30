package example;

import configs.ConfigModuleCollection;
import pipeline.ExecutableStep;
import pipeline.ExecutionManager;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ConfigModules modules = new ConfigModules();

        modules.merge(new File("/home/nico/Software/pipeJar/src/main/resources/configs.json"));

        ExecutableStep first = new FirstStep();
        ExecutableStep second = new SecondStep(first);

        ExecutionManager manager = new ExecutionManager(first, second);
        manager.run();
    }

    protected static class ConfigModules extends ConfigModuleCollection {
        public static final TestModule testModule = new TestModule();
    }
}

package example;

import configs.ConfigModuleCollection;
import configs.ConfigTypes.FileTypes.OutputFile;
import pipeline.ExecutionManager;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ExecutionManager.setThreadNumber(1);
        ExecutionManager.workingDirectory = new OutputFile("/home/nico/Data/PipeJar");

        ConfigModules modules = new ConfigModules();

        modules.merge(new File("/home/nico/Software/pipeJar/src/main/resources/configs.json"));

        FirstStep first = new FirstStep();
        SecondStep second = new SecondStep(first);

        ExecutionManager manager = new ExecutionManager(first, second);
        manager.run();
    }

    protected static class ConfigModules extends ConfigModuleCollection {
        public static final TestModule testModule = new TestModule();
    }
}

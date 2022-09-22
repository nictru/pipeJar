package example;

import configs.ConfigModuleCollection;

import static pipeline.ExecutionManager.*;

public class Main {
    public static void main(String[] args) {
        new ConfigModules();
        
        addSteps(new TestStep());

        if (simulate())
            run();
    }

    protected static class ConfigModules extends ConfigModuleCollection {
        public static final TestModule testModule = new TestModule();
    }
}

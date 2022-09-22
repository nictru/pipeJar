package pipeline;

import configs.ConfigTypes.InputTypes.InputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExecutionManager {
    private static final Logger logger = LogManager.getLogger(ExecutionManager.class);
    private static Set<InputConfig<File>> createdFiles = new HashSet<>();
    private static List<ExecutableStep> steps;

    public static void addSteps(ExecutableStep... addSteps) {
        steps = List.of(addSteps);
    }

    public static boolean simulate() {
        logger.info("Starting simulation");
        boolean allGood = true;

        for (ExecutableStep step : steps) {
            if (!step.simulate()) {
                allGood = false;
            }
        }

        if (!allGood)
            logger.error("Simulation failed.");
        else
            logger.info("Simulation successful.");

        return allGood;
    }

    public static void run() {
        for (ExecutableStep step : steps) {
            step.run();
        }
    }

    public static Set<InputConfig<File>> getRegisteredFileStructure() {
        return createdFiles;
    }

    public static void registerCreatedFileStructure(InputConfig<File> file) {
        createdFiles.add(file);
    }

    public static int getThreadLimit() {
        return 1;
    }

    public static int getMemoryLimitMb() {
        return 4000;
    }

    public static boolean getDevelopmentMode() {
        return true;
    }
}

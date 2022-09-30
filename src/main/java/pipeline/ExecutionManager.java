package pipeline;

import configs.ConfigTypes.InputTypes.InputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

public class ExecutionManager {
    static final ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    private static final Logger logger = LogManager.getLogger(ExecutionManager.class);
    private static final Set<ExecutableStep> steps = new HashSet<>();
    static ExecutionStates state = ExecutionStates.WAITING;
    private static Set<InputConfig<File>> createdFiles = new HashSet<>();

    public static void addSteps(ExecutableStep... steps) {
        ExecutionManager.steps.addAll(List.of(steps));
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

    private static boolean waitForAll(Function<ExecutableStep, Future<Boolean>> function, String name) {
        logger.info("Waiting for " + name + " results...");

        boolean allGood = steps.stream().map(function).allMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        });

        if (!allGood) {
            logger.error(name + " failed.");
            setState(ExecutionStates.FAILED);
        } else
            logger.info(name + " finished successfully.");

        return allGood;
    }

    public static boolean execute() {
        setState(ExecutionStates.EXECUTION);

        return waitForAll(ExecutableStep::getExecutionFuture, "Execution");
    }

    public static boolean simulate() {
        setState(ExecutionStates.SIMULATION);
        return waitForAll(ExecutableStep::getSimulationFuture, "Simulation");
    }

    private static void setState(ExecutionStates state) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        logger.info("State was set to " + state);
        if (state.equals(ExecutionStates.FAILED)) {
            System.exit(0);
        }
        ExecutionManager.state = state;
    }

    public static void shutdown() {
        executorService.shutdown();
    }
}

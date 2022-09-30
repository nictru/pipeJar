package pipeline;

import configs.ConfigTypes.InputTypes.InputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

public class ExecutionManager {
    final static ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    private final Logger logger = LogManager.getLogger(ExecutionManager.class);
    private final List<ExecutableStep> steps;
    private Set<InputConfig<File>> createdFiles = new HashSet<>();

    public ExecutionManager(ExecutableStep... steps) {
        this.steps = sortSteps(new HashSet<>(List.of(steps)));
    }

    public Set<InputConfig<File>> getRegisteredFileStructure() {
        return createdFiles;
    }

    public void registerCreatedFileStructure(InputConfig<File> file) {
        createdFiles.add(file);
    }

    public void run() {
        if (simulate()) {
            execute();
        }
        shutdown();
    }

    public boolean execute() {
        return waitForAll(ExecutableStep::execute, "Execution");
    }

    public boolean simulate() {
        return waitForAll(ExecutableStep::simulate, "Simulation");
    }


    public void shutdown() {
        executorService.shutdown();
    }

    private List<ExecutableStep> sortSteps(Set<ExecutableStep> unsortedSteps) {
        HashSet<ExecutableStep> addedSteps = new HashSet<>();
        List<ExecutableStep> sortedSteps = new ArrayList<>();

        while (unsortedSteps.size() > 0) {
            HashSet<ExecutableStep> freshAddedSteps = new HashSet<>();

            for (ExecutableStep step : unsortedSteps) {
                if (addedSteps.containsAll(step.getDependencies())) {
                    freshAddedSteps.add(step);
                    addedSteps.add(step);
                    sortedSteps.add(step);
                }
            }

            freshAddedSteps.forEach(unsortedSteps::remove);

            if (freshAddedSteps.isEmpty()) {
                logger.error("Could not define execution order.");
                throw new IllegalArgumentException("Could not define execution order. Steps that could not be placed: " + unsortedSteps);
            }
        }

        return sortedSteps;
    }

    private boolean waitForAll(Function<ExecutableStep, Future<Boolean>> function, String name) {
        logger.info("Waiting for " + name + " results...");

        boolean allGood = steps.parallelStream().map(function).allMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        });

        if (!allGood) {
            logger.error(name + " failed.");
        } else
            logger.info(name + " finished successfully.");

        return allGood;
    }
}

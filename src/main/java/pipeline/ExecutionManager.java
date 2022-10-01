package pipeline;

import configs.ConfigTypes.FileTypes.OutputFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;

public class ExecutionManager {
    public static OutputFile workingDirectory;

    private static Integer threadNumber;
    private static ThreadPoolExecutor executorService;
    private final Logger logger = LogManager.getLogger(ExecutionManager.class);
    private final List<ExecutableStep> steps;

    public ExecutionManager(ExecutableStep... steps) {
        this.steps = sortSteps(new HashSet<>(List.of(steps)));
        if (new StyleChecker().check(this.steps)) {
            logger.info("Style checks finished successfully.");
        } else {
            logger.error("Style checks finished with problems.");
            System.exit(0);
        }
    }

    public static Integer getThreadNumber() {
        return threadNumber;
    }

    public static void setThreadNumber(int nThreads) {
        if (threadNumber != null) {
            System.out.println("Thread number has already been set!");
            return;
        }
        threadNumber = nThreads;
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);
    }

    static Future<Boolean> submit(Callable<Boolean> callable) {
        return executorService.submit(callable);
    }

    public void run() {
        if (simulate()) {
            execute();
        }
        shutdown();
    }

    public void execute() {
        waitForAll(ExecutableStep::execute, "Execution");
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

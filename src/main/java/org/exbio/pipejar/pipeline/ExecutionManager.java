package pipeline;

import configs.ConfigTypes.FileTypes.OutputFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class ExecutionManager {
    private static final ExecutorService chillPool = Executors.newCachedThreadPool();
    public static OutputFile workingDirectory;
    private static Integer threadNumber;
    private static ExecutorService performancePool;
    private static boolean hashingEnabled = true;
    private final Logger logger = LogManager.getLogger(ExecutionManager.class);
    private final Collection<ExecutableStep> steps;

    public ExecutionManager(ExecutableStep... steps) {
        this.steps = new HashSet<>(List.of(steps));
        if (new StyleChecker().check(this.steps)) {
            logger.info("Style checks finished successfully.");
        } else {
            logger.error("Style checks finished with problems.");
            System.exit(0);
        }
        this.steps.forEach(step -> step.getOutputs().forEach(OutputFile::register));
    }

    public static Integer getThreadNumber() {
        return threadNumber;
    }

    public static void setThreadNumber(int nThreads) {
        if (threadNumber != null) {
            System.out.println("Thread number has already been set!");
            return;
        }
        if (nThreads < 1) {
            System.out.println("Thread number has to be at least 1");
            System.exit(1);
        }
        threadNumber = nThreads;
        performancePool = Executors.newFixedThreadPool(nThreads);
    }

    static Future<Boolean> submitPerformanceTask(Callable<Boolean> callable) {
        return performancePool.submit(callable);
    }

    static Future<Boolean> submitEasyTask(Callable<Boolean> callable) {
        return chillPool.submit(callable);
    }

    public static void disableHashing() {
        hashingEnabled = false;
    }

    static boolean isHashingEnabled() {
        return hashingEnabled;
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
        performancePool.shutdown();
        chillPool.shutdown();
    }

    private boolean waitForAll(Function<ExecutableStep, Future<Boolean>> function, String name) {
        logger.info("Waiting for " + name + " results...");

        Collection<Future<Boolean>> futures = new HashSet<>();

        steps.forEach(step -> futures.add(function.apply(step)));

        boolean allGood = futures.stream().allMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                logger.warn(e.getMessage());
                return false;
            }
        });

        if (!allGood) {
            logger.error(name + " failed.");
        } else {
            logger.info(name + " finished successfully.");
        }

        return allGood;
    }
}

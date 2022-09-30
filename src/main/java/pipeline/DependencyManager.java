package pipeline;

import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public class DependencyManager {
    private final Set<ExecutableStep> dependencies;
    private final Logger logger;

    public DependencyManager(Logger logger, Collection<ExecutableStep> dependencies) {
        this.logger = logger;
        this.dependencies = new HashSet<>(dependencies);
    }

    private boolean waitForState(ExecutionStates state) {
        logger.debug("Waiting for state: " + state);

        while (!ExecutionManager.state.equals(ExecutionStates.FAILED) && !ExecutionManager.state.equals(state)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }

        return !ExecutionManager.state.equals(ExecutionStates.FAILED);
    }

    boolean waitForSimulations() {
        if (!waitForState(ExecutionStates.SIMULATION)) {
            return false;
        }
        return waitForFutures(ExecutableStep::getSimulationFuture, "simulation");
    }

    boolean waitForExecution() {
        if (!waitForState(ExecutionStates.EXECUTION)) {
            return false;
        }

        return waitForFutures(ExecutableStep::getExecutionFuture, "execution");
    }

    private boolean waitForFutures(Function<ExecutableStep, Future<Boolean>> function, String name) {
        logger.debug("Waiting for dependency " + name + " results...");

        return dependencies.stream().map(function).allMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        });
    }
}

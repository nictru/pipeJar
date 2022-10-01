package pipeline;

import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public record DependencyManager(Collection<ExecutableStep> dependencies, Logger logger) {

    boolean waitForExecution() {
        return waitForDependencies(ExecutableStep::getExecutionFuture, "execution");
    }

    boolean waitForSimulation() {
        return waitForDependencies(ExecutableStep::getSimulationFuture, "simulation");
    }

    private boolean waitForDependencies(Function<ExecutableStep, Future<Boolean>> func, String process) {
        if (dependencies.isEmpty()) {
            return true;
        }
        logger.debug("Waiting for " + dependencies.size() + " " + process + (dependencies.size() == 1 ? " dependency." : "dependencies."));
        boolean result = dependencies.stream().map(func).allMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        });

        if (result) {
            logger.debug("All dependencies finished their " + process + " successfully.");
        } else {
            logger.warn("There were problems with " + process + " dependencies.");
        }
        return result;
    }
}

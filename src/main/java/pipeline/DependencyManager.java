package pipeline;

import configs.ConfigTypes.FileTypes.OutputFile;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class DependencyManager {
    private final Collection<OutputFile> dependencies;
    private final Logger logger;

    public DependencyManager(Collection<OutputFile> dependencies, Logger logger) {
        this.dependencies = dependencies;
        this.logger = logger;
    }

    synchronized boolean waitForExecution() {
        return waitForDependencies(OutputFile.states.Created, OutputFile.states.ErrorDuringCreation, "execution");
    }

    synchronized boolean waitForSimulation() {
        return waitForDependencies(OutputFile.states.WillBeCreated, OutputFile.states.WillNotBeCreated, "simulation");
    }

    private boolean waitForDependencies(OutputFile.states targetState, OutputFile.states problemState, String process) {
        if (dependencies.isEmpty()) {
            return true;
        }

        boolean successfull;

        logger.debug("Waiting for " + dependencies.size() + " " + process +
                (dependencies.size() == 1 ? " dependency." : " dependencies."));

        while (!((successfull = allMatch(targetState)) || anyMatch(problemState) ||
                dependencies.stream().anyMatch(OutputFile::isNotRegistered))) {
            try {
                this.wait();
            } catch (InterruptedException | IllegalMonitorStateException e) {
                logger.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        if (successfull) {
            logger.debug("All dependencies finished their " + process + " successfully.");
        } else {
            logger.warn("There were problems with " + process + " dependencies:");
            dependencies.forEach(dependency -> {
                if (dependency.isNotRegistered()) {
                    logger.warn("\t" + dependency.getAbsolutePath() +
                            " will not be generated because its creation step will not be executed.");
                }
                if (dependency.getState().equals(OutputFile.states.ErrorDuringCreation)) {
                    logger.warn("\t" + dependency.getAbsolutePath() + " had an error during creation.");
                }
                if (dependency.getState().equals(OutputFile.states.WillNotBeCreated)) {
                    logger.warn("\t" + dependency.getAbsolutePath() +
                            " will not be generated because of a certain config situation.");
                }
            });
        }
        return successfull;
    }

    private boolean allMatch(OutputFile.states state) {
        return dependencies.stream().allMatch(dependency -> dependency.getState().equals(state));
    }

    private boolean anyMatch(OutputFile.states state) {
        return dependencies.stream().anyMatch(dependency -> dependency.getState().equals(state));
    }

    public synchronized void notifyUpdate() {
        this.notifyAll();
    }

    Collection<OutputFile> getDependencies() {
        return dependencies;
    }
}

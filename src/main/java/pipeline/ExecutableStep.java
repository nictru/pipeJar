package pipeline;

import configs.ConfigTypes.FileTypes.InputFile;
import configs.ConfigTypes.FileTypes.OutputFile;
import configs.ConfigTypes.UsageTypes.UsageConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ExecutionTimeMeasurement;
import util.FileManagement;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static pipeline.ExecutionManager.executorService;
import static util.FileManagement.deleteFileStructure;
import static util.FileManagement.makeSureDirectoryExists;

public abstract class ExecutableStep implements EventListener {
    /**
     * The logger of this ExecutableStep.
     */
    protected final Logger logger = LogManager.getLogger(this.getClass());
    private final DependencyManager dependencyManager;
    private final OutputFile workingDirectory;
    private final HashManager hashManager;
    private Future<Boolean> simulationFuture, executionFuture;

    protected ExecutableStep(ExecutableStep... dependencies) {
        workingDirectory = new OutputFile(ExecutionManager.workingDirectory, this.getClass().getName().replace(".", "_"));

        try {
            makeSureDirectoryExists(workingDirectory);
        } catch (IOException e) {
            logger.warn("Could not create working directory: " + e.getMessage());
        }

        dependencyManager = new DependencyManager(List.of(dependencies), logger);
        hashManager = new HashManager(workingDirectory, logger);
    }

    protected void updateOutputFiles() {
        try {
            for (Field outputFileField : getOutputFileFields()) {
                String old = outputFileField.get(this).toString();
                outputFileField.set(this, new OutputFile(workingDirectory, old));
            }
        } catch (IllegalAccessException e) {
            logger.error("Illegal access: " + e.getMessage());
        }
    }

    private Collection<Field> getOutputFileFields() {
        Set<Field> outputFiles = new HashSet<>();

        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(OutputFile.class)) {
                outputFiles.add(field);
            }
        }

        return outputFiles;
    }

    public Future<Boolean> getSimulationFuture() {
        return simulationFuture;
    }

    public Future<Boolean> getExecutionFuture() {
        return executionFuture;
    }

    public Collection<ExecutableStep> getDependencies() {
        return dependencyManager.dependencies();
    }


    /**
     * Check if all the requirements of this executableStep are met and broadcast the created file structures. Does
     * not execute the executableStep.
     *
     * @return true if the simulation was successful, otherwise false.
     */
    Future<Boolean> simulate() {
        simulationFuture = executorService.submit(() -> {
            if (!dependencyManager.waitForSimulation()) {
                return false;
            }

            logger.debug("Simulation starting.");

            if (checkRequirements()) {
                logger.debug("Simulation successful.");
                return createFiles();
            } else {
                return false;
            }
        });
        return simulationFuture;
    }

    private boolean createFiles() {
        logger.debug("Creating output files.");

        return getOutputFileFields().stream().map(field -> {
            try {
                return (OutputFile) field.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access: " + e.getMessage());
            }
        }).allMatch(outputFile -> {
            if (!outputFile.exists()) {
                try {
                    return outputFile.createNewFile();
                } catch (IOException e) {
                    logger.warn("Could not create file: " + e.getMessage());
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Wraps the executableStep execution with some framework checks.
     * <p>
     * Skips the executableStep if developmentMode is not active and valid hashes are found.
     * Stores new hashes if the executableStep has been executed and developmentMode is disabled.
     */
    Future<Boolean> execute() {
        executionFuture = executorService.submit(() -> {
            if (!dependencyManager.waitForExecution()) {
                return false;
            }

            logger.info("Fetching callables.");
            Set<Callable<Boolean>> callables = getCallables();

            if (callables == null || callables.size() == 0) {
                logger.error("No callables found");
                return false;
            } else {
                logger.info("Found " + callables.size() + " supplier(s).");
            }

            logger.debug("Execution starting.");

            ExecutionTimeMeasurement timer = new ExecutionTimeMeasurement();

            boolean successful;

            if (!hashManager.validateHashes(getConfigs(), getInputFiles())) {
                logger.debug("Hash is invalid.");

                successful = callables.stream().map(executorService::submit).allMatch(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.warn(e.getMessage());
                        return false;
                    }
                });

                hashManager.writeHashes(getConfigs(), getInputFiles());
            } else {
                successful = true;
                logger.debug("Skipped execution since hash is valid.");
            }

            logger.info("Finished. Step took " + timer.stopAndGetDeltaFormatted());

            return successful;
        });
        return executionFuture;
    }

    /**
     * Get the configs that are not mandatory for execution of this executableStep but influence the outcome.
     * Generally if a Config.isSet() check takes place before config value usage, it is an optional config.
     *
     * @return a set of the optional configs, must not be null.
     */
    private <T> Collection<T> getFieldObjects(Class<T> clazz) {
        Collection<T> configs = new HashSet<>();
        for (Field field : this.getClass().getDeclaredFields()) {

            if (field.getType().getSuperclass().equals(clazz)) {
                try {
                    configs.add((T) field.get(this));
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return configs;
    }

    private Collection<UsageConfig> getConfigs() {
        return getFieldObjects(UsageConfig.class);
    }

    private Collection<InputFile> getInputFiles() {
        return getFieldObjects(InputFile.class);
    }

    /**
     * Check if the file structure and config requirements of this executableStep are met.
     * <p>
     * The following requirements are checked:
     * <ul>
     *     <li>All the required file structures are contained by the getCreatedFileStructure( set</li>
     *     <li>All the required configs are set</li>
     * </ul>
     * <p>
     * Logs a warning, if a single requirement is not met.
     *
     * @return true if all the requirements are met, otherwise false
     */
    private boolean checkRequirements() {
        return getConfigs().stream().allMatch(config -> {
            boolean result = !config.isRequired() || config.isSet();
            if (!result) {
                logger.warn("A required config is not set: " + config.getName());
            }
            return result;
        });
    }

    /**
     * The job performed by this executableStep.
     * <p>
     * If the main job consists of multiple sub jobs that require the previous sub job to be finished, splitting the
     * process into multiple executableSteps should be considered. If this is not an option, the
     * finishAllQueuedThreads() method should be used in order to make sure that the previous sub job is finished.
     */
    protected abstract Set<Callable<Boolean>> getCallables();

    protected InputFile input(OutputFile outputFile) {
        InputFile inputFile = new InputFile(workingDirectory, outputFile);
        try {
            deleteFileStructure(inputFile);
            FileManagement.softLink(inputFile, outputFile);
        } catch (IOException e) {
            logger.warn("Could not creat soft link: " + e.getMessage());
        }

        return inputFile;
    }
}

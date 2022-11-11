package org.exbio.pipejar.pipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.UsageConfig;
import org.exbio.pipejar.util.ExecutionTimeMeasurement;
import org.exbio.pipejar.util.FileManagement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.exbio.pipejar.util.FileManagement.deleteFileStructure;
import static org.exbio.pipejar.util.FileManagement.makeSureDirectoryExists;

public abstract class ExecutableStep implements EventListener {
    /**
     * The logger of this ExecutableStep.
     */
    protected final Logger logger = LogManager.getLogger(this.getClass());
    protected final OutputFile inputDirectory, outputDirectory;
    private final DependencyManager dependencyManager;
    private final HashManager hashManager;

    private final Collection<OutputFile> outputs = new HashSet<>();
    private final Collection<InputFile> inputs = new HashSet<>();

    protected ExecutableStep(Collection<OutputFile> dependencies) {
        OutputFile workingDirectory =
                new OutputFile(ExecutionManager.workingDirectory, this.getClass().getName().replace(".", "_"));
        inputDirectory = new OutputFile(workingDirectory, "input");
        outputDirectory = new OutputFile(workingDirectory, "output");

        try {
            makeSureDirectoryExists(workingDirectory);
            makeSureDirectoryExists(inputDirectory);
            makeSureDirectoryExists(outputDirectory);
        } catch (IOException e) {
            logger.warn("Could not create working directory: " + e.getMessage());
        }

        dependencyManager = new DependencyManager(dependencies, logger);
        hashManager = new HashManager(workingDirectory, logger, inputDirectory, outputDirectory);

        dependencies.forEach(this::addInput);
    }

    protected ExecutableStep(OutputFile... dependencies) {
        this(List.of(dependencies));
    }

    public Collection<OutputFile> getOutputs() {
        return outputs;
    }

    protected Collection<InputFile> getInputs() {
        return inputs;
    }

    /**
     * Check if all the requirements of this executableStep are met and broadcast the created file structures. Does
     * not execute the executableStep.
     *
     * @return true if the simulation was successful, otherwise false.
     */
    Future<Boolean> simulate() {
        return ExecutionManager.submitEasyTask(() -> {
            if (!dependencyManager.waitForSimulation()) {
                return false;
            }

            logger.trace("Simulation starting.");

            if (checkRequirements()) {
                logger.debug("Simulation successful.");
                markOutputsAs(OutputFile.states.WillBeCreated);
                boolean result = createFiles();
                logger.trace("Finished creating files.");
                return result;
            } else {
                logger.warn("Simulation failed.");
                markOutputsAs(OutputFile.states.WillNotBeCreated);
                return false;
            }
        });
    }

    private void markOutputsAs(OutputFile.states state) {
        if (!outputs.isEmpty()) {
            logger.trace("Marking outputs as " + state);
            outputs.forEach(output -> output.setState(state));
        }
    }

    private boolean createFiles() {
        if (!doCreateFiles()) {
            logger.debug("Skipping pre-creation of output files since manually deactivated.");
            return true;
        }

        logger.debug("Creating output files.");

        return outputs.stream().allMatch(outputFile -> {
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

    /***
     * Override this method, if files should not be pre-created during simulation
     * @return true if files should be pre-created, false otherwise
     */
    protected boolean doCreateFiles() {
        return true;
    }

    /**
     * Wraps the executableStep execution with some framework checks.
     * <p>
     * Skips the executableStep if developmentMode is not active and valid hashes are found.
     * Stores new hashes if the executableStep has been executed and developmentMode is disabled.
     */
    Future<Boolean> execute() {
        return ExecutionManager.submitEasyTask(() -> {
            if (!dependencyManager.waitForExecution()) {
                return false;
            }

            logger.info("Fetching callables.");
            Collection<Callable<Boolean>> callables = getCallables();

            if (callables == null || callables.size() == 0) {
                logger.error("No callables found");
                return false;
            } else {
                logger.info("Found " + callables.size() + " callable(s).");
            }

            ExecutionTimeMeasurement timer = new ExecutionTimeMeasurement();

            boolean successful;

            if (!mayBeSkipped() || !ExecutionManager.isHashingEnabled() || !hashManager.validateHashes(getConfigs())) {
                logger.debug("Execution starting.");

                successful =
                        callables.parallelStream().map(ExecutionManager::submitPerformanceTask).allMatch(future -> {
                            try {
                                return future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                logger.warn(e.getMessage());
                                return false;
                            }
                        });

                if (mayBeSkipped() && ExecutionManager.isHashingEnabled()) {
                    hashManager.writeHashes(getConfigs());
                }
            } else {
                successful = true;
                logger.debug("Skipped execution since hash is valid.");
            }

            logger.info("Finished. Step took " + timer.stopAndGetDeltaFormatted());

            if (successful) {
                markOutputsAs(OutputFile.states.Created);
            } else {
                markOutputsAs(OutputFile.states.ErrorDuringCreation);
            }

            return successful;
        });
    }

    /**
     * Get the configs that are not mandatory for execution of this executableStep but influence the outcome.
     * Generally if a Config.isSet() check takes place before config value usage, it is an optional config.
     *
     * @return a set of the optional configs, must not be null.
     */
    private Collection<UsageConfig<?>> getConfigs() {
        Collection<UsageConfig<?>> configs = new HashSet<>();
        for (Field field : this.getClass().getDeclaredFields()) {

            if (field.getType().getSuperclass().equals(UsageConfig.class)) {
                try {
                    configs.add((UsageConfig<?>) field.get(this));
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return configs;
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
    protected abstract Collection<Callable<Boolean>> getCallables();

    protected InputFile addInput(UsageConfig<File> file) {
        if (!file.isSet()) {
            throw new IllegalArgumentException("Cannot add not-set file as input.");
        }
        InputFile inputFile = new InputFile(inputDirectory, file.get().getName());
        try {
            deleteFileStructure(inputFile);
            FileManagement.softLink(inputFile, file.get());
        } catch (IOException e) {
            logger.warn("Could not creat soft link: " + e.getMessage());
        }

        return inputFile;
    }

    protected InputFile addInput(OutputFile outputFile) {
        InputFile inputFile = new InputFile(inputDirectory, outputFile);
        try {
            FileManagement.softLink(inputFile, outputFile);
        } catch (IOException e) {
            logger.warn("Could not creat soft link: " + e.getMessage());
        }

        inputs.add(inputFile);
        outputFile.addListener(this.dependencyManager);

        return inputFile;
    }

    protected boolean mayBeSkipped() {
        return true;
    }

    protected OutputFile addOutput(String name) {
        OutputFile output = new OutputFile(outputDirectory, name);
        outputs.add(output);

        return output;
    }

    Collection<OutputFile> getDependencies() {
        return dependencyManager.getDependencies();
    }
}

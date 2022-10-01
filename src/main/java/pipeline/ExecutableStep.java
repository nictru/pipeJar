package pipeline;

import configs.ConfigTypes.FileTypes.InputFile;
import configs.ConfigTypes.FileTypes.OutputFile;
import configs.ConfigTypes.InputTypes.InputConfig;
import configs.ConfigTypes.UsageTypes.OptionalConfig;
import configs.ConfigTypes.UsageTypes.RequiredConfig;
import configs.ConfigTypes.UsageTypes.UsageConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ExecutionTimeMeasurement;
import util.FileManagement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static pipeline.ExecutionManager.executorService;
import static util.FileManagement.deleteFileStructure;
import static util.FileManagement.readLines;
import static util.Hashing.hashFiles;

/**
 * Abstract class for a single analysis performed as part of the pipeline.
 * <p>
 * The following practices should be applied to all extending classes:
 * <ul>
 *     <li>All the used configs should be stored as private final class data elements in the beginning of the class</li>
 *     <li>The configs should be split to four blocks:
 *     <ol>
 *         <li>Required file structure (input files/directories)</li>
 *         <li>Created file structure (output files/directories)</li>
 *         <li>Required configs (mandatory configs for this executableStep)</li>
 *         <li>Optional configs (not mandatory but influencing the output)</li>
 *     </ol>
 *     </li>
 *     <li>Each config has to be assigned to one of the following methods:
 *     <ul>
 *         <li>{@link #getRequiredFileStructure}</li>
 *         <li>{@link #getRequiredConfigs}</li>
 *         <li>{@link #getOptionalConfigs}</li>
 *     </ul>
 *     </li>
 *     <li>If a file structure or config is not required or created if a certain config constellation is active,
 *     the config constellation should be modelled inside the corresponding get method. The get methods should model
 *     the real execution requirements and outputs as exact as possible.</li>
 * </ul>
 */
public abstract class ExecutableStep implements EventListener {
    /**
     * The logger of this ExecutableStep.
     */
    protected final Logger logger = LogManager.getLogger(this.getClass());
    private final DependencyManager dependencyManager;
    private final OutputFile workingDirectory;
    private Future<Boolean> simulationFuture, executionFuture;

    protected ExecutableStep(ExecutableStep... dependencies) {
        this.dependencyManager = new DependencyManager(List.of(dependencies), logger);
        workingDirectory = new OutputFile(ExecutionManager.workingDirectory, this.getClass().getName().replace(".", "_"));

        if (workingDirectory.exists()) {
            logger.info("Deleting working directory");
            try {
                deleteFileStructure(workingDirectory);
            } catch (IOException e) {
                logger.error("Could not delete old working directory.");
            }
        }

        if (!workingDirectory.mkdir()) {
            logger.error("Could not create working directory");
        }
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
            try {
                return outputFile.createNewFile();
            } catch (IOException e) {
                logger.warn("Could not create file: " + e.getMessage());
            }
            return false;
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

            logger.debug("Verifying hash...");
            if (!verifyHash()) {
                logger.debug("Hash is invalid.");

                successful = callables.stream().map(executorService::submit).allMatch(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.warn(e.getMessage());
                        return false;
                    }
                });
                logger.debug("Writing hash");
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
     * Get the file configs whose value files or directories must have been created before execution of this
     * executableStep.
     *
     * @return a set of the required file structure configs, must not be null.
     */
    protected Set<InputConfig<File>> getRequiredFileStructure() {
        return new HashSet<>();
    }

    /**
     * Get the configs that are mandatory for execution of this executableStep.
     *
     * @return a set of the required configs, must not be null.
     */
    protected Set<RequiredConfig<?>> getRequiredConfigs() {
        Set<RequiredConfig<?>> configs = new HashSet<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(RequiredConfig.class)) {
                try {
                    configs.add((RequiredConfig<?>) field.get(this));
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return configs;
    }

    /**
     * Get the configs that are not mandatory for execution of this executableStep but influence the outcome.
     * Generally if a Config.isSet() check takes place before config value usage, it is an optional config.
     *
     * @return a set of the optional configs, must not be null.
     */
    protected Set<OptionalConfig<?>> getOptionalConfigs() {
        Set<OptionalConfig<?>> configs = new HashSet<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(OptionalConfig.class)) {
                try {
                    configs.add((OptionalConfig<?>) field.get(this));
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
        boolean allGood = true;
        Set<UsageConfig<?>> configs = new HashSet<>();
        configs.addAll(getRequiredConfigs());
        configs.addAll(getOptionalConfigs());

        for (UsageConfig<?> config : configs) {
            if (config.isRequired() && !config.isSet()) {
                allGood = false;
                logger.warn("A required config is not set: " + config.getName());
            }
        }
        return allGood;
    }


    /**
     * Hash the input directories of this executableStep.
     *
     * @return hash string
     * @throws IOException if the input directories cannot be read.
     */
    private String hashInputs() throws IOException {
        Set<InputConfig<File>> requiredFileStructure = getRequiredFileStructure();
        ArrayList<File> requiredFiles = new ArrayList<>();

        for (InputConfig<File> fileConfig : requiredFileStructure) {
            requiredFiles.add(fileConfig.get());
        }

        return hashFiles(requiredFiles);
    }

    /**
     * Hash the required configs of this executableStep.
     * Required configs have to be set in order to ensure the functionality of this executableStep
     *
     * @return hash string
     */
    private String hashRequiredConfigs() {
        Set<RequiredConfig<?>> requiredConfigs = getRequiredConfigs();
        return util.Hashing.hashConfigs(requiredConfigs);
    }

    /**
     * Hash the optional configs of this executableStep.
     * Optional configs are configs which are checked if they are set, before their value is being used.
     *
     * @return hash string
     */
    private String hashOptionalConfigs() {
        Set<OptionalConfig<?>> optionalConfigs = getOptionalConfigs();
        return util.Hashing.hashConfigs(optionalConfigs);
    }

    /**
     * Calculate the hashes of the found file structures and configs and compare them to the stored values.
     *
     * @return true if the hashes match, false if mismatch
     */
    public boolean verifyHash() {
        try {
            File hashFile = getHashFile();
            if (!hashFile.exists() || !hashFile.canRead()) {
                logger.debug("Cannot read hash file.");
                return false;
            }
            List<String> content = readLines(hashFile);
            assert content.size() == 4;

            String oldInputHash = content.get(0);
            String oldRequiredConfigHash = content.get(1);
            String oldOptionalConfigHash = content.get(2);

            String inputHash = hashInputs();
            if (!inputHash.equals(oldInputHash)) {
                logger.warn("Input changed");
                return false;
            }

            String configHash = hashRequiredConfigs();
            if (!configHash.equals(oldRequiredConfigHash)) {
                logger.warn("Required configs changed");
                return false;
            }

            String optionalConfigHash = hashOptionalConfigs();
            if (!optionalConfigHash.equals(oldOptionalConfigHash)) {
                logger.warn("Optional configs changed.");
                return false;
            }

            return true;
        } catch (IOException | AssertionError e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get the hash file for this executableStep.
     *
     * @return the hash file
     */
    private File getHashFile() {
        return new File(this.getClass().getName() + ".md5");
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
            FileManagement.softLink(inputFile, outputFile);
        } catch (IOException e) {
            logger.warn("Could not creat soft link: " + e.getMessage());
        }

        return inputFile;
    }
}

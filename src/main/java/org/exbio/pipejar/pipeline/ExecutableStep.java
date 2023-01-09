package org.exbio.pipejar.pipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.UsageConfig;
import org.exbio.pipejar.util.ExecutionTimeMeasurement;
import org.exbio.pipejar.util.FileManagement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
    private boolean skip = false;

    protected ExecutableStep(boolean add, OutputFile... dependencies) {
        this(add, new HashSet<>(), dependencies);
    }

    public ExecutableStep() {
        this(false, (OutputFile) null);
    }

    protected ExecutableStep(boolean add, Collection<OutputFile> dependencies) {
        this(add, dependencies, (OutputFile) null);
    }

    public ExecutableStep(boolean add, Collection<OutputFile>... dependencies) {
        this(add, Arrays.stream(dependencies).flatMap(Collection::stream).toArray(OutputFile[]::new));
    }

    protected ExecutableStep(boolean add, Collection<OutputFile> dependencies, OutputFile... otherDependencies) {
        Collection<OutputFile> combined = new HashSet<>(dependencies) {{
            Arrays.stream(otherDependencies).filter(Objects::nonNull).forEach(this::add);
        }};
        OutputFile workingDirectory =
                new OutputFile(ExecutionManager.workingDirectory, this.getClass().getName().replace(".", "_"));
        inputDirectory = new OutputFile(workingDirectory, "input");
        outputDirectory = new OutputFile(workingDirectory, "output");

        try {
            deleteFileStructure(inputDirectory);
            makeSureDirectoryExists(workingDirectory);
            makeSureDirectoryExists(inputDirectory);
            makeSureDirectoryExists(outputDirectory);
        } catch (IOException e) {
            logger.warn("Could not create working directory: " + e.getMessage());
        }

        dependencyManager = new DependencyManager(combined, logger);
        hashManager = new HashManager(workingDirectory, logger, inputDirectory, outputDirectory);

        if (add) {
            combined.forEach(this::addInput);
        }
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
                if (mayBeSkipped() && ExecutionManager.isHashingEnabled() && hashManager.validateHashes(getConfigs())) {
                    skip = true;
                } else {
                    deleteFileStructure(outputDirectory);
                    makeSureDirectoryExists(outputDirectory);
                }
                boolean result = createFiles();
                logger.trace(result ? "Successfully finished creating files." : "Failed to create files.");
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
                    File parent = outputFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        logger.warn("Could not ensure that parent directory exists: " + parent.getAbsolutePath());
                    }

                    if (outputFile.getName().contains(".")) {
                        return outputFile.createNewFile();
                    } else {
                        return outputFile.mkdir();
                    }
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

            ExecutionTimeMeasurement timer = new ExecutionTimeMeasurement();

            boolean successful;

            if (!skip) {
                logger.info("Fetching callables.");
                Collection<Callable<Boolean>> callables = getCallables();

                if (callables == null || callables.size() == 0) {
                    logger.error("No callables found");
                    return false;
                } else {
                    logger.info("Found " + callables.size() + " callable(s).");
                }

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

                if (successful) {
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

            if (field.getType().getSuperclass() != null && field.getType().getSuperclass().equals(UsageConfig.class)) {
                try {
                    field.setAccessible(true);
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

            logger.trace("Checking config: " + config.getName());
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
        OutputFile outputFile = new OutputFile(file.get().getAbsolutePath());
        return addInput(outputFile);
    }

    protected InputFile addInput(OutputFile outputFile) {
        return addInput(inputDirectory, outputFile);
    }

    protected InputFile addInput(OutputFile parent, OutputFile outputFile) {
        InputFile inputFile = new InputFile(parent, outputFile);
        try {
            FileManagement.softLink(inputFile, outputFile);
        } catch (IOException e) {
            logger.warn("Could not creat soft link: " + e.getMessage());
        }

        inputs.add(inputFile);
        outputFile.addListener(this.dependencyManager);

        return inputFile;
    }

    protected InputFile addInput(InputStream stream, String name) {
        InputFile target = new InputFile(inputDirectory, name);
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(stream.readAllBytes());
        } catch (IOException e) {
            logger.warn("Could not copy stream to file: " + e.getMessage());
        }
        return target;
    }

    protected boolean mayBeSkipped() {
        return true;
    }

    protected OutputFile addOutput(String name) {
        return addOutput(this.outputDirectory, name);
    }

    protected OutputFile addOutput(OutputFile parent, String name) {
        OutputFile output = new OutputFile(parent, name);
        outputs.add(output);

        return output;
    }

    Collection<OutputFile> getDependencies() {
        return dependencyManager.getDependencies();
    }

    public void copyResources(String source, final Path target) throws URISyntaxException, IOException {
        URI resource = Objects.requireNonNull(getClass().getResource("")).toURI();

        try (FileSystem fileSystem = FileSystems.newFileSystem(
                resource,
                Collections.<String, String>emptyMap()
        )) {
            final Path jarPath = fileSystem.getPath(source);

            Files.walkFileTree(jarPath, new SimpleFileVisitor<>() {
                private Path currentTarget;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    currentTarget = target.resolve(jarPath.relativize(dir).toString());
                    Files.createDirectories(currentTarget);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().endsWith(".class")) {
                        Files.copy(file, target.resolve(jarPath.relativize(file).toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}

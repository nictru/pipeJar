package org.exbio.pipejar.pipeline;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.configs.ConfigModuleCollection;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static org.exbio.pipejar.util.FileManagement.extend;

public abstract class Workflow<C extends ConfigModuleCollection> {
    private final Collection<ExecutableStep> steps = new HashSet<>();
    public File workingDirectory;
    public C configs;
    protected final Logger logger = LogManager.getLogger(this.getClass());


    public Workflow(String[] args) throws IOException, ParseException {
        ArgParser argParser = new ArgParser(args);
        init(argParser);
        buildFlow();
        execute();
    }

    private void init(ArgParser argParser) throws IOException {
        File configFile = argParser.getConfigFile();
        workingDirectory = argParser.getWorkingDirectory();
        ExecutionManager.workingDirectory = new OutputFile(extend(workingDirectory, "output").getAbsolutePath());
        ExecutionManager.setThreadNumber(argParser.getThreadNumber());

        configs = createConfigs();

        if (!configs.merge(configFile) || !configs.validate()) {
            System.exit(1);
        }
        configs.save(extend(workingDirectory, "configs.json"));
    }

    protected abstract C createConfigs();

    protected abstract void buildFlow();
    private void execute() {
        ExecutionManager manager = new ExecutionManager(steps);
        manager.run();
    }

    protected <T extends ExecutableStep> T add(T step) {
        steps.add(step);
        return step;
    }
}

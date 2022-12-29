package org.exbio.pipejar.pipeline;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.UsageConfig;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.util.FileManagement;
import org.exbio.pipejar.util.Hashing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class HashManager {
    private final Logger logger;

    private final File configHashFile, inputHashFile, outputHashFile;

    private final String oldConfigHash, oldInputHash, oldOutputHash;
    private final File inputDirectory, outputDirectory;

    public HashManager(File superDirectory, Logger logger, File inputDirectory, File outputDirectory) {
        this.logger = logger;
        OutputFile workingDirectory = new OutputFile(superDirectory, ".hashes");
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;

        configHashFile = new File(workingDirectory, "configs.md5");
        inputHashFile = new File(workingDirectory, "inputs.md5");
        outputHashFile = new File(workingDirectory, "outputs.md5");

        oldConfigHash = readHash(configHashFile);
        oldInputHash = readHash(inputHashFile);
        oldOutputHash = readHash(outputHashFile);
    }


    private String readHash(File hashFile) {
        if (hashFile.exists() && hashFile.canRead()) {
            try {
                return FileManagement.readFile(hashFile);
            } catch (IOException e) {
                logger.warn("Could not read hash file: " + hashFile.getAbsolutePath());
            }
        }

        return "";
    }

    boolean validateHashes(Collection<UsageConfig<?>> configs) {
        logger.debug("Validating hash...");

        if (oldConfigHash.isEmpty() || oldInputHash.isEmpty() || oldOutputHash.isEmpty()) {
            logger.debug("No hashes found, skipping validation");
            return false;
        }

        String configHash = hashConfigs(configs);
        boolean configMatches = oldConfigHash.equals(configHash);
        if (!configMatches) {
            logger.debug("Configs changed. Newly calculated hash: " + configHash);
        }

        String inputHash = hashDirectory(inputDirectory);
        boolean inputMatches = oldInputHash.equals(inputHash);
        if (!inputMatches) {
            logger.debug("Input changed. Newly calculated hash: " + inputHash);
        }

        String outputHash = hashDirectory(outputDirectory);
        boolean outputMatches = oldOutputHash.equals(outputHash);
        if (!outputMatches) {
            logger.debug("Output changed. Newly calculated hash: " + outputHash);
        }

        return configMatches && inputMatches && outputMatches;
    }

    void writeHashes(Collection<UsageConfig<?>> configs) throws IOException {
        logger.debug("Writing hashes.");

        FileManagement.writeFile(configHashFile, hashConfigs(configs));
        FileManagement.writeFile(inputHashFile, hashDirectory(inputDirectory));
        FileManagement.writeFile(outputHashFile, hashDirectory(outputDirectory));
    }

    private String hashConfigs(Collection<UsageConfig<?>> configs) {
        return Hashing.hash(
                configs.stream().map(UsageConfig::toString).map(Hashing::hash).sorted().collect(Collectors.joining()));
    }

    private String hashDirectory(File file) {
        try {
            return file.exists() ? Hashing.hashFile(file) : "";
        } catch (IOException e) {
            logger.warn("Could not calculate input hash");
            throw new RuntimeException(e);
        }
    }
}

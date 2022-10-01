package pipeline;

import configs.ConfigTypes.FileTypes.OutputFile;
import configs.ConfigTypes.UsageTypes.UsageConfig;
import org.apache.logging.log4j.Logger;
import util.FileManagement;
import util.Hashing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class HashManager {
    private final Logger logger;

    private final File configHashFile, inputHashFile;

    private final String oldConfigHash, oldInputHash;

    public HashManager(File superDirectory, Logger logger) {
        this.logger = logger;
        OutputFile workingDirectory = new OutputFile(superDirectory, ".hashes");

        configHashFile = new File(workingDirectory, "configs.md5");
        inputHashFile = new File(workingDirectory, "inputs.md5");

        oldConfigHash = readHash(configHashFile);
        oldInputHash = readHash(inputHashFile);
    }


    private String readHash(File hashFile) {
        if (hashFile.exists() && hashFile.canRead()) {
            try {
                logger.debug("Reading hash from " + hashFile);
                return FileManagement.readFile(hashFile);
            } catch (IOException e) {
                logger.warn("Could not read hash file: " + hashFile.getAbsolutePath());
            }
        }

        return "";
    }

    boolean validateHashes(Collection<UsageConfig> configs, Collection<? extends File> inputFiles) {
        logger.debug("Validating hash...");
        return oldConfigHash.equals(hashConfigs(configs)) && oldInputHash.equals(hashFiles(inputFiles));
    }

    void writeHashes(Collection<UsageConfig> configs, Collection<? extends File> inputFiles) throws IOException {
        logger.debug("Writing hashes to " + configHashFile + " and " + inputHashFile);

        FileManagement.writeFile(configHashFile, hashConfigs(configs));
        FileManagement.writeFile(inputHashFile, hashFiles(inputFiles));
    }

    private String hashConfigs(Collection<UsageConfig> configs) {
        return Hashing.hash(configs.stream().map(UsageConfig::toString).map(Hashing::hash).sorted().collect(Collectors.joining()));
    }

    private String hashFiles(Collection<? extends File> files) {
        try {
            return Hashing.hashFiles(new ArrayList<>(files));
        } catch (IOException e) {
            logger.warn("Could not calculate input hash");
            throw new RuntimeException(e);
        }
    }
}

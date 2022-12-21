package org.exbio.pipejar.configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;
import org.exbio.pipejar.pipeline.ExecutionManager;
import org.exbio.pipejar.util.FileManagement;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for config modules.
 * <p>
 * Contains configs and other modules (submodules).
 */
public abstract class ConfigModule {
    protected final Logger logger = LogManager.getLogger(this.getClass().getName());

    /**
     * Maps the config names to their objects.
     * <p>
     * Required for json export.
     * The odd name is due to naming overlap with the {@link ConfigModuleCollection} class.
     */
    protected Map<String, InputConfig<?>> entries = new HashMap<>();

    /**
     * Maps all the submodule names inside this class to their objects.
     * <p>
     * Required for json export.
     */
    protected Map<String, ConfigModule> subModules = new HashMap<>();

    /**
     * Fills the entry and submodule maps and does the same for all submodules.
     */
    protected void init() {
        try {
            logger.debug("Initializing config module");
            registerEntries();
            initSubmodules();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Calls the default constructor of all the submodules and registers them inside the submodule map.
     */
    private void initSubmodules()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Get all fields inside the class extending the ConfigModule class
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            Class<?> superClass = field.getType().getSuperclass();

            // If the field superclass is ConfigModule, then it is a submodule
            if (superClass != null && superClass.equals(ConfigModule.class)) {
                // Call the default constructor with the same argument as this object has been created
                ConfigModule module = (ConfigModule) field.getType().getConstructor().newInstance();
                // Assign the created object to this object
                field.set(this, module);

                // Add the created object to the submodule map
                subModules.put(field.getType().getSimpleName(), module);
                module.init();
            }
        }
    }

    /**
     * Add all created configs to the entry map.
     */
    private void registerEntries() throws IllegalAccessException {
        // Get all fields of the class extending AbstractModule
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            // Check if the field is a Config
            if (InputConfig.class.isAssignableFrom(field.getType())) {
                // Add the config to the entry map
                entries.put(field.getName(), (InputConfig<?>) field.get(this));
                ((InputConfig<?>) field.get(this)).setName(field.getName());
            }
        }
    }

    /**
     * Merge a config json object into the module.
     *
     * @param mergeObject the json object to merge
     * @return true if merging is successful, otherwise false
     */
    public boolean merge(JSONObject mergeObject) {
        boolean worked = true;
        for (String key : mergeObject.keySet()) {
            if (subModules.containsKey(key)) {
                // Merge configs to a submodule
                try {
                    subModules.get(key).merge(mergeObject.getJSONObject(key));
                } catch (ClassCastException e) {
                    worked = false;
                    logger.warn(this.getClass().getSimpleName() + ": " + key + ": " + e.getMessage());
                }
            } else if (entries.containsKey(key)) {
                // Set config value
                try {
                    entries.get(key).setValueObject(mergeObject.get(key));
                } catch (IllegalAccessException | ClassCastException | IllegalArgumentException | IOException e) {
                    worked = false;
                    logger.warn(e.getMessage());
                }
            } else {
                // Key is neither a submodule nor an entry
                worked = false;
                logger.warn(this.getClass().getSimpleName() + ": Trying to set unknown config: " + key);
                logger.warn("Known configs: " + entries.keySet() + ", known submodules: " + subModules.keySet());
            }
        }
        return worked;
    }

    /**
     * Merge an external config file to the configs object.
     *
     * @param configFile the config file
     * @throws IOException if the config file cannot be read
     */
    public boolean merge(File configFile) throws IOException {
        logger.debug("Merging configuration file: " + configFile.getAbsolutePath());
        String content = FileManagement.readFile(configFile);

        try {
            JSONObject combined = new JSONObject(content);
            boolean worked = merge(combined);
            logger.info("Merged configuration file: " + configFile.getAbsolutePath());
            return worked;
        } catch (JSONException e) {
            logger.error("The config JSON-File does not match the JSON format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a json object containing the configs inside this instance and all submodules.
     *
     * @param onlyWriteable defines if only writeable configs should be included
     * @return the json object
     */
    public JSONObject toJSONObject(boolean onlyWriteable, boolean excludeFiles) {
        JSONObject combined = new JSONObject();

        for (String key : subModules.keySet()) {
            JSONObject subModuleJSONObject = subModules.get(key).toJSONObject(onlyWriteable, excludeFiles);
            if (!subModuleJSONObject.keySet().isEmpty()) {
                combined.accumulate(key, subModuleJSONObject);
            }
        }
        for (String key : entries.keySet()) {
            InputConfig<?> entry = entries.get(key);

            if (onlyWriteable && !entry.isWriteable() ||
                    excludeFiles && entry.isSet() && entry.get().getClass().equals(File.class)) {
                continue;
            }

            combined.accumulate(key, entry.toJSONifyAble());
        }

        return combined;
    }

    /**
     * Checks if the configs inside this module are valid. Does the same for all submodules.
     * <p>
     * If development mode is active and file configs are sub file structures of the working directory, they are
     * added to the {@link ExecutionManager} createdFileStructure. This allows commenting out executableSteps from the
     * workspace without running into errors, if the files have already been created in an earlier pipeline execution
     * or are copied from another run
     *
     * @return true if all configs are valid, otherwise false
     */
    public boolean validate() {
        boolean subModulesValid = true;
        for (ConfigModule subModule : subModules.values()) {
            subModulesValid = subModule.validate() && subModulesValid;
        }

        boolean configsValid = true;
        for (InputConfig<?> config : entries.values()) {
            boolean thisValid = config.isValid(logger);
            configsValid = thisValid && configsValid;

            if (thisValid && config.isSet() && config.get().getClass().equals(File.class)) {
                InputConfig<File> fileConfig = (InputConfig<File>) config;
                if (fileConfig.isSet() && !fileConfig.get().exists()) {
                    logger.warn("File does not exist: " + fileConfig.get().getAbsolutePath() + " (" + fileConfig.getName() + ")");
                    configsValid = false;
                }
            }
        }
        return subModulesValid && configsValid;
    }
}

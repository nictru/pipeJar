package configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import util.FileManagement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static util.FileManagement.writeFile;

public abstract class ConfigModuleCollection {
    /**
     * Maps the module names to their objects
     */
    private final Map<String, ConfigModule> configs = new HashMap<>();
    private final Logger logger;

    /**
     * The default constructor.
     */
    public ConfigModuleCollection() {
        logger = LogManager.getLogger(this.getClass());

        // Iterate all the fields inside this class
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> superClass = field.getType().getSuperclass();

            // Check if the field extends AbstractModule
            if (superClass != null && superClass.equals(ConfigModule.class)) {
                try {
                    // Call the AbstractModule constructor
                    ConfigModule module = (ConfigModule) field.get(this);
                    module.init();

                    // Store the created module in the name - object map
                    configs.put(field.getType().getSimpleName(), module);
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Merge an external config file to the configs object.
     *
     * @param configFile the config file
     * @throws IOException if the config file cannot be read
     */
    public void merge(File configFile) throws IOException {
        logger.debug("Merging configuration file: " + configFile.getAbsolutePath());
        String content = FileManagement.readFile(configFile);
        JSONObject combined = new JSONObject();
        boolean allModulesWorked = true;

        try {
            combined = new JSONObject(content);
        } catch (JSONException e) {
            logger.error("The config JSON-File does not match the JSON formant: " + e.getMessage());
        }

        for (String moduleName : combined.keySet()) {
            JSONObject moduleJSONObject = combined.getJSONObject(moduleName);

            if (configs.containsKey(moduleName)) {
                ConfigModule module = configs.get(moduleName);
                allModulesWorked = module.merge(moduleJSONObject) && allModulesWorked;
            } else {
                logger.warn("Trying to set config for unknown module: " + moduleName);
            }
        }
        if (!allModulesWorked) {
            logger.error("There were errors during config file merging. Aborting.");
        }
        logger.info("Merged configuration file: " + configFile.getAbsolutePath());
    }

    /**
     * Get a json string of all the stored configs (not only writable).
     *
     * @return the json string
     */
    public String toString() {
        return getConfigsJSONString(false);
    }

    /**
     * Get a json string of the stored configs.
     *
     * @param onlyWriteable defines if only writeable configs should be added to the config string
     * @return the json string
     */
    private String getConfigsJSONString(boolean onlyWriteable) {
        return getConfigsJSONObject(onlyWriteable).toString(4);
    }

    /**
     * Get a JSONObject of all the stored configs.
     *
     * @param onlyWriteable defines if only writeable configs should be added to the config string
     * @return the JSONObject
     */
    public JSONObject getConfigsJSONObject(boolean onlyWriteable) {
        return getConfigsJSONObject(onlyWriteable, false);
    }

    public JSONObject getConfigsJSONObject(boolean onlyWriteable, boolean excludeFiles) {
        JSONObject combined = new JSONObject();

        for (String key : configs.keySet()) {
            JSONObject module = configs.get(key).toJSONObject(onlyWriteable, excludeFiles);
            if (!module.isEmpty()) {
                combined.accumulate(key, module);
            }
        }

        return combined;
    }

    /**
     * Validate the configs inside all the modules.
     */
    public void validate() {
        logger.info("Validating configs");
        boolean allValid = true;
        for (ConfigModule module : configs.values()) {
            allValid = module.validate() && allValid;
        }
        if (allValid) {
            logger.info("Configs are valid.");
        } else {
            logger.error("Configs are invalid. Aborting.");
        }
    }

    /**
     * Save the configs to a file in json format
     */
    public void save(File saveFile) {
        try {
            writeFile(saveFile, getConfigsJSONString(true));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}

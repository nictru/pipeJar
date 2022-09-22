package configs.ConfigTypes.InputTypes;

import configs.ConfigTypes.Config;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * A single generic config containing an object which has an effect on the pipeline execution.
 *
 * @param <T> the data type of the config
 */
public abstract class InputConfig<T> extends Config<T> {
    private T value;
    String name;

    public abstract boolean isWriteable();

    public abstract boolean isValid(Logger logger);

    public abstract boolean isSet();

    public void setName(String name) {
        this.name = name;
    }

    public T get() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public abstract void setValueObject(Object value) throws ClassCastException, IOException, IllegalAccessException;

    public Object toJSONifyAble() {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value.getClass().equals(File.class)) {
            return ((File) value).getAbsolutePath();
        }
        return value;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "{NULL}";
        }

        if (value.getClass().equals(File.class)) {
            return ((File) value).getAbsolutePath();
        }

        return value.toString();
    }
}

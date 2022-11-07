package configs.ConfigTypes.InputTypes;

import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class InternalConfig<T> extends InputConfig<T> {
    public InternalConfig(T value) {
        setValue(value);
    }

    @Override
    public boolean isWriteable() {
        return false;
    }

    @Override
    public boolean isValid(Logger logger) {
        return true;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public void setValueObject(Object value) throws ClassCastException, IOException, IllegalAccessException {
        throw new IllegalAccessException("Trying to set an internal config via config file: " + name);
    }
}

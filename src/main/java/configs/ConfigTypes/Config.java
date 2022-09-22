package configs.ConfigTypes;

import org.apache.logging.log4j.Logger;

public abstract class Config<T> {
    public abstract boolean isValid(Logger logger);

    public abstract boolean isSet();

    public abstract String getName();

    public abstract T get();
}

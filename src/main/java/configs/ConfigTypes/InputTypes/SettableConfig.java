package configs.ConfigTypes.InputTypes;

import configs.ConfigValidators.Validator;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.*;

public class SettableConfig<T> extends InputConfig<T> {
    Set<Validator<T>> validators = new HashSet<>();

    private final Class<?> configClass;

    @SafeVarargs
    public SettableConfig(Class<? extends T> configClass, Validator<T>... validators) {
        this.configClass = configClass;
        this.validators.addAll(List.of(validators));
    }

    @SafeVarargs
    public SettableConfig(T value, Validator<T>... validators) {
        this((Class<? extends T>) value.getClass(), validators);
        setValue(value);
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    public boolean isValid(Logger logger) {
        boolean allValidatorsPassed = true;

        for (Validator<T> validator : validators) {
            boolean passed = false;
            try {
                passed = validator.validate(this);
            } catch (ClassCastException e) {
                logger.warn("Object of type " + get().getClass() + " cannot by validated by " + validator);
            }
            if (!passed) {
                logger.warn(name + " does not match requirements: " + validator);
            }
            allValidatorsPassed = allValidatorsPassed && passed;
        }

        return allValidatorsPassed;
    }

    @Override
    public boolean isSet() {
        return get() != null;
    }

    @Override
    public void setValueObject(Object value) throws ClassCastException {
        if (value.getClass().equals(configClass)) {
            setValue((T) value);
        } else if ((configClass.equals(List.class) || configClass.equals(ArrayList.class)) &&
                value.getClass().equals(JSONArray.class)) {
            List<?> bigDecimalList = ((JSONArray) value).toList();
            if (bigDecimalList.size() > 0 && bigDecimalList.get(0).getClass().equals(BigDecimal.class)) {
                List<Double> doubleList = new ArrayList<>();
                for (Object bigDecimalValue : bigDecimalList) {
                    doubleList.add(Double.valueOf(((BigDecimal) bigDecimalValue).doubleValue()));
                }
                setValue((T) doubleList);
            } else {
                setValue((T) bigDecimalList);
            }
        } else if (configClass.equals(Map.class) && value.getClass().equals(JSONObject.class)) {
            setValue((T) ((JSONObject) value).toMap());
        } else if (configClass.equals(Double.class) && value.getClass().equals(BigDecimal.class)) {
            setValue((T) Double.valueOf(((BigDecimal) value).doubleValue()));
        } else if (configClass.equals(Double.class) && value.getClass().equals(Integer.class)) {
            double doubleValue = ((Integer) value).intValue();
            setValue((T) (Double) doubleValue);
        } else if (value == JSONObject.NULL) {
            setValue(null);
        } else {
            throw new ClassCastException(
                    "Trying to set a value of type " + value.getClass() + " to a config with type " + configClass +
                            ": " + getName());
        }
    }
}

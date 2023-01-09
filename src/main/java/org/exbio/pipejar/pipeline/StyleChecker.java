package org.exbio.pipejar.pipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.OptionalConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.UsageConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StyleChecker {
    private final Logger logger = LogManager.getLogger(this.getClass());

    public boolean check(Collection<ExecutableStep> steps) {
        return checkSteps(steps) && checkExecutionManager();
    }

    private boolean checkExecutionManager() {
        if (ExecutionManager.workingDirectory == null) {
            logger.warn("The execution manager working directory needs to be set manually.");
            return false;
        }
        if (ExecutionManager.getThreadNumber() == null) {
            logger.warn("The execution manager thread number needs to be set manually.");
            return false;
        }
        return true;
    }

    private boolean checkSteps(Collection<ExecutableStep> steps) {
        return steps.stream().allMatch(this::checkStep);
    }

    private boolean checkStep(ExecutableStep step) {
        Collection<Function<Field, Boolean>> checks = new HashSet<>() {{
            // Input file modifiers
            add(field -> {
                if (!field.getType().equals(InputFile.class)) {
                    return true;
                }
                int modifier = field.getModifiers();
                if (!Modifier.isPrivate(modifier) || !Modifier.isFinal(modifier)) {
                    logger.warn("InputFile \"" + field.getName() + "\" in " + step.getClass().getName() +
                            " must be private and final.");
                    return false;
                }
                return true;
            });
            // Config modifiers
            add(field -> {
                if (field.getType().getSuperclass() == null || !field.getType().getSuperclass().equals(UsageConfig.class)) {
                    return true;
                }
                int modifier = field.getModifiers();
                if (!Modifier.isPrivate(modifier) || !Modifier.isFinal(modifier)) {
                    logger.warn(field.getClass().getSimpleName() + " \"" + field.getName() + "\" in " +
                            step.getClass().getName() + " must be private and final.");
                    return false;
                }
                return true;
            });
            // TODO: Add check for ConfigModule init calling in constructor
        }};

        return Arrays.stream(step.getClass().getDeclaredFields()).allMatch(
                field -> checks.stream().allMatch(fieldBooleanFunction -> fieldBooleanFunction.apply(field)));
    }
}

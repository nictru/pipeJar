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
                if (Modifier.isPrivate(field.getModifiers()) ||
                        !field.getType().getSuperclass().equals(UsageConfig.class)) {
                    return true;
                }
                int modifier = field.getModifiers();
                if (!Modifier.isPublic(modifier) || !Modifier.isFinal(modifier)) {
                    logger.warn(field.getClass().getSimpleName() + " \"" + field.getName() + "\" in " +
                            step.getClass().getName() + " must be public and final.");
                    return false;
                }
                return true;
            });
            // Output file modifiers and updating
            add(field -> {
                if (!field.getType().equals(OutputFile.class)) {
                    return true;
                }
                int modifier = field.getModifiers();
                if (!Modifier.isPublic(modifier) || !Modifier.isFinal(modifier)) {
                    logger.warn("OutputFile \"" + field.getName() + "\" in " + step.getClass().getName() +
                            " must be public and final.");
                    return false;
                }
                try {
                    OutputFile outputFile = (OutputFile) field.get(step);
                    if (!step.getOutputs().contains(outputFile)) {
                        logger.warn("Outputfile \"" + field.getName() +
                                "\" has not been created using the \"addOutput()\" method in " +
                                step.getClass().getName());
                        return false;
                    }
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage());
                    return false;
                }
                return true;
            });
            // Field type
            add(field -> {
                Set<Class<?>> allowedTypes = new HashSet<>(
                        List.of(OptionalConfig.class, RequiredConfig.class, InputFile.class, OutputFile.class));
                if (!Modifier.isPrivate(field.getModifiers()) && !allowedTypes.contains(field.getType())) {
                    logger.warn("Field \"" + field.getName() + "\" in " + step.getClass().getName() +
                            " has a forbidden type: " + field.getType().getName() + ". Allowed types are: " +
                            allowedTypes.stream().map(Class::getSimpleName).collect(Collectors.toSet()));
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

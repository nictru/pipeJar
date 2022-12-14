package org.exbio.pipejar.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScriptExecution
{
    public static void executeAndWait(File file, boolean redirectOutput) throws IOException {
        Process process = execute(file, redirectOutput);
        waitFor(process, List.of(file.getAbsolutePath()));
    }

    public static void executeAndWait(List<String> command, boolean redirectOutput) throws IOException {
        Process process = execute(command, new HashMap<>(), redirectOutput);
        waitFor(process, command);
    }

    private static Process executeProcessBuilder(ProcessBuilder builder) throws IOException {
        return builder.start();
    }

    public static void executeAndWait(String command, boolean redirectOutput) throws IOException {
        executeAndWait(command, new HashMap<>(), redirectOutput);
    }

    public static void executeAndWait(String command, HashMap<String, String> environment, boolean redirectOutput) throws IOException {
        Process process = execute(command, environment, redirectOutput);
        int returnCode = waitFor(process, new ArrayList<>(List.of(command)));

        if (returnCode != 0)
        {
            throw new RuntimeException("Received return code " + returnCode + "\n\n Command was: " + command);
        }
    }

    public static void executeAndWait(String executable, String fileExtension, boolean redirectOutput) throws IOException {
        List<String> command = getExecutionCommand(executable, fileExtension);

        executeAndWait(command, redirectOutput);
    }

    private static int waitFor(Process process, List<String> command) throws IOException {
        try
        {
            int returnCode = process.waitFor();
            if (returnCode != 0)
            {
                throw new IOException("Received return code " + returnCode + " Command was: " + command);
            }
            return returnCode;
        } catch (InterruptedException | IOException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    private static List<String> getExecutionPrefix(String fileExtension, boolean fileExecution)
    {
        List<String> command = new ArrayList<>();
        switch (fileExtension) {
            case ".R" -> command.add("Rscript");
            case ".py" -> {
                command.add("python3");
                if (!fileExecution) {
                    command.add("-c");
                }
            }
            case ".sh" -> {
                command.add("sh");
            }
            default -> throw new RuntimeException("This file type is not supported.");
        }

        return command;
    }

    private static List<String> getExecutionCommand(String executable, String fileExtension)
    {
        List<String> command = getExecutionPrefix(fileExtension, false);
        command.add(executable);
        return command;
    }

    private static List<String> getExecutionCommand(File file)
    {
        String extension = file.getName().substring(file.getName().lastIndexOf("."));
        List<String> command = getExecutionPrefix(extension, true);
        command.add(file.getAbsolutePath());
        return command;
    }

    public static Process execute(String command, boolean redirectOutput) throws IOException {
        return execute(command, new HashMap<>(), redirectOutput);
    }

    public static Process execute(String command, HashMap<String, String> environment, boolean redirectOutput) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        if (redirectOutput)
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        setEnvironment(builder, environment);
        Process process = executeProcessBuilder(builder);
        assert process != null;
        return process;
    }

    public static Process execute(List<String> command, HashMap<String, String> environment, boolean redirectOutput) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (redirectOutput)
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        setEnvironment(builder, environment);
        Process process = executeProcessBuilder(builder);
        assert process != null;
        return process;
    }

    public static Process execute(List<String> command, boolean redirectOutput) throws IOException {
        return execute(command, new HashMap<>(), redirectOutput);
    }

    public static Process execute(File file, boolean redirectOutput) throws IOException {
        List<String> command = getExecutionCommand(file);
        return execute(command, redirectOutput);
    }

    private static void setEnvironment(ProcessBuilder builder, HashMap<String, String> environment)
    {
        builder.environment().putAll(environment);
    }
}

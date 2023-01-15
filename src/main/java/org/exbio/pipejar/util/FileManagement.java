package org.exbio.pipejar.util;

import org.exbio.pipejar.util.FileFilters.Filters;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileManagement {

    public static String readFile(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    public static List<String> readLines(File file) throws IOException {
        List<String> raw = Files.readAllLines(file.toPath());
        List<String> trimmed = new ArrayList<>();
        for (String rawLine : raw) {
            trimmed.add(rawLine.replace("\n", "").replace("\r", ""));
        }
        return trimmed;
    }

    public static void copyFile(File source, File target) throws IOException {
        target.getParentFile().mkdirs();
        Files.copy(source.toPath(), target.toPath(), REPLACE_EXISTING);
    }

    public static void copyDirectory(File source, File target) throws IOException {
        copyDirectory(source, target, file -> true);
    }

    public static void copyDirectory(File source, File target, FileFilter filter) throws IOException {
        for (File sourceFile : Objects.requireNonNull(source.listFiles(filter))) {
            if (sourceFile.isFile()) {
                copyFile(sourceFile, extend(target, sourceFile.getName()));
            } else {
                copyDirectory(sourceFile, extend(target, sourceFile.getName()), filter);
            }
        }
    }

    public static void writeHTML(File file, String content, int relativationDepth) throws IOException {
        content = content.replace("{RELATIVATION}", (".." + File.separator).repeat(relativationDepth));

        writeFile(file, content);
    }

    public static void writeFile(File file, String content) throws IOException {
        makeSureFileExists(file);
        if (file.isFile()) {
            Files.writeString(file.toPath(), content);
        } else {
            System.out.println(file.getAbsolutePath());
            throw new IllegalArgumentException("Can only process files, not directories!");
        }
    }

    public static String findValueInTable(String term, int searchIndex, int resultIndex, File file, String sep,
                                          boolean ignoreCase) throws FileNotFoundException, NoSuchFieldException {
        if (file.isFile()) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.split(sep).length > searchIndex && line.split(sep).length > resultIndex) {

                        if (term.equals(line.split(sep)[searchIndex]) ||
                                ignoreCase && term.equalsIgnoreCase(line.split(sep)[searchIndex])) {
                            return line.split(sep)[resultIndex];
                        }
                    }
                }
            }
        }
        throw new NoSuchFieldException(
                "Could not find term \"" + term + "\" in column " + searchIndex + " of " + file.getAbsolutePath());
    }

    public static File getFileIfInDirectory(File directory, String fileNameRegex, boolean lookingForFiles) {
        if (directory == null) {
            return null;
        }

        if (directory.listFiles() == null) {
            return null;
        }

        for (File entry : Objects.requireNonNull(
                directory.listFiles(lookingForFiles ? Filters.fileFilter : Filters.directoryFilter))) {
            if (entry.getName().matches(fileNameRegex)) {
                return entry;
            }
        }
        return null;
    }

    public static synchronized void makeSureFileExists(File file) throws IOException {
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs()) {
                        throw new IOException("parent directory");
                    }
                }
                if (!file.createNewFile()) {
                    throw new IOException("file");
                }
            } catch (IOException e) {
                throw new IOException(
                        "Exception during creation of: " + file.getAbsolutePath() + " (" + e.getMessage() + ")");
            }
        }
    }


    public static synchronized void makeSureDirectoryExists(File directory) throws IOException {
        if (directory.isFile()) {
            throw new IllegalArgumentException("Can not handle files. Received: " + directory.getAbsolutePath());
        }
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Could not create directory: " + directory.getAbsolutePath());
            }
        }
    }


    public static void appendToFile(File file, String content) throws IOException {
        makeSureFileExists(file);

        try {
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IOException("Could not append content to file: " + file.getAbsolutePath());
        }
    }

    public static File extend(File file, String... extensions) {
        File extended = new File(file.getAbsolutePath());

        for (String extension : extensions) {
            extended = new File(extended.getAbsolutePath() + File.separator + extension);
        }
        return extended;
    }

    public static boolean isEmpty(File file) throws IOException {
        String content = readFile(file);
        return content.isBlank();
    }

    public static void hardLink(File newLink, File existingData) throws IOException {
        makeSureDirectoryExists(newLink.getParentFile());

        if (newLink.exists()) {
            deleteFileStructure(newLink);
        }
        Files.createLink(newLink.toPath(), existingData.toPath());
    }


    public static void softLink(File newLink, File existingData) throws IOException {
        makeSureDirectoryExists(newLink.getParentFile());

        if (newLink.exists()) {
            Files.delete(newLink.toPath());
        }

        Path source = existingData.toPath();
        Path link = newLink.toPath();
        Path relativeSource = link.getParent().relativize(source);

        Files.createSymbolicLink(link, relativeSource);
    }


    public static void deleteFileStructure(File file) throws IOException {
        if (Files.isSymbolicLink(file.toPath())) {
            Files.delete(file.toPath());
            return;
        }

        if (file.exists()) {
            if (file.isDirectory()) {
                for (File subFile : Objects.requireNonNull(file.listFiles())) {
                    deleteFileStructure(subFile);
                }
            }
            Files.delete(file.toPath());
        }
    }

    public static boolean recreateDirectory(File file) throws IOException {
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("Cannot handle files. Only directories allowed!");
        }

        if (file.exists()) {
            deleteFileStructure(file);
        }

        return file.mkdir();
    }

    public static void makeAllChildrenExecutable(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                makeAllChildrenExecutable(child);
            }
        }
        file.setExecutable(true);
    }
}

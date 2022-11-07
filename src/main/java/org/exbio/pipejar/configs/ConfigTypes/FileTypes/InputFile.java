package org.exbio.pipejar.configs.ConfigTypes.FileTypes;

import java.io.File;

public class InputFile extends File {
    public InputFile(String pathname) {
        super(pathname);
    }

    public InputFile(File parent, String child) {
        super(parent, child);
    }

    public InputFile(OutputFile workingDirectory, OutputFile outputFile) {
        this(workingDirectory, outputFile.getName());
    }
}

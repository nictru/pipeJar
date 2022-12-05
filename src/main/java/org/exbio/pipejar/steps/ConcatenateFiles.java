package org.exbio.pipejar.steps;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.pipeline.ExecutableStep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.FileManagement.readLines;

public class ConcatenateFiles extends ExecutableStep {
    public final OutputFile outputFile;

    public ConcatenateFiles(Collection<OutputFile> dependencies) {
        super(true, dependencies);
        outputFile = addOutput("concatenated.txt");
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                    getInputs().stream().sorted().map(inputFile -> {
                        try {
                            return readLines(inputFile);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).forEachOrdered(lines -> lines.forEach(line -> {
                        try {
                            writer.write(line);
                            writer.newLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                }

                return true;
            });
        }};
    }
}

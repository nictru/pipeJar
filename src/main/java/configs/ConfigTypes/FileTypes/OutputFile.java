package configs.ConfigTypes.FileTypes;

import java.io.File;

public class OutputFile extends File {
    public OutputFile(String pathname) {
        super(pathname);
    }

    public OutputFile(File parent, String child) {
        super(parent, child);
    }
}

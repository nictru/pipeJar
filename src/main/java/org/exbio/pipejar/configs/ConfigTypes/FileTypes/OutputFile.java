package org.exbio.pipejar.configs.ConfigTypes.FileTypes;

import org.exbio.pipejar.pipeline.DependencyManager;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public class OutputFile extends File {
    private final Collection<DependencyManager> listeners = new HashSet<>();
    private states state = states.Pending;
    private boolean registered = false;

    public OutputFile(String pathname) {
        super(pathname);
    }

    public OutputFile(File parent, String child) {
        super(parent, child);
    }

    public boolean isNotRegistered() {
        return !registered;
    }

    public void register() {
        this.registered = true;
    }

    public states getState() {
        return state;
    }

    public void setState(states state) {
        this.state = state;
        listeners.forEach(DependencyManager::notifyUpdate);
    }

    public void addListener(DependencyManager dependencyManager) {
        listeners.add(dependencyManager);
    }

    public enum states {
        Pending, WillNotBeCreated, WillBeCreated, Created, ErrorDuringCreation
    }
}

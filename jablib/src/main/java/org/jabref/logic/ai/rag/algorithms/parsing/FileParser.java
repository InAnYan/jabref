package org.jabref.logic.ai.rag.algorithms;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

public interface FileParser {
    Optional<String> parse(Path path, ReadOnlyBooleanProperty shutdownSignal);
}

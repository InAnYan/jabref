package org.jabref.logic.ai.rag.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.util.LongTaskInfo;

public interface FileParser {
    Optional<String> parse(LongTaskInfo longTaskInfo, Path path);
}

package org.jabref.logic.ai.pipeline.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.util.LongTaskInfo;

public interface FileContentParser {
    Optional<String> parse(LongTaskInfo longTaskInfo, Path path);
}

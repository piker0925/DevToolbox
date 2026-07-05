package com.back.domain.tool;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ToolInput(List<Path> files, Map<String, String> params) {}

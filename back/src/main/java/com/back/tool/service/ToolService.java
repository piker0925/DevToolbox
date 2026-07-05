package com.back.tool.service;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import com.back.tool.model.ToolModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final List<ToolModule> moduleList;
    private Map<String, ToolModule> modules;

    @PostConstruct
    void init() {
        modules = moduleList.stream()
                .collect(Collectors.toMap(ToolModule::getId, m -> m));
    }

    public List<ToolModule> listModules() {
        return moduleList;
    }

    public ToolModule getModule(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId))
                .orElseThrow(() -> new AppException(ErrorCode.MODULE_NOT_FOUND));
    }
}

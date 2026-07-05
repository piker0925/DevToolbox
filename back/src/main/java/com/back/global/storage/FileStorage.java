package com.back.global.storage;

import java.nio.file.Path;

public interface FileStorage {
    void save(String key, Path localFile);
    String getUrl(String key);
    void delete(String key);
}

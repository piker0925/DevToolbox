package com.back.global.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorage {
    void save(String key, Path localFile);
    String getUrl(String key);
    void delete(String key);

    InputStream openStream(String key) throws IOException;
}

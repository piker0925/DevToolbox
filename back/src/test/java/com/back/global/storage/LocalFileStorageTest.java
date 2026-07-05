package com.back.global.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080");
    }

    @Test
    void save_createsFileAtExpectedPath() throws IOException {
        Path source = Files.createTempFile("source", ".txt");
        Files.writeString(source, "hello");

        storage.save("job-abc/result.txt", source);

        assertThat(tempDir.resolve("job-abc/result.txt")).exists();
        assertThat(Files.readString(tempDir.resolve("job-abc/result.txt"))).isEqualTo("hello");
    }

    @Test
    void getUrl_returnsHttpUrl() {
        String url = storage.getUrl("job-abc/result.pdf");
        assertThat(url).isEqualTo("http://localhost:8080/api/v1/files/job-abc/result.pdf");
    }

    @Test
    void delete_removesFile() throws IOException {
        Path source = Files.createTempFile("source", ".txt");
        Files.writeString(source, "data");
        storage.save("job-xyz/result.txt", source);

        storage.delete("job-xyz/result.txt");

        assertThat(tempDir.resolve("job-xyz/result.txt")).doesNotExist();
    }
}

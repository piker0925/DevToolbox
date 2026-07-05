package com.back.job.controller;

import com.back.global.exception.AppException;
import com.back.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class FileController {

    private final Path uploadDir;

    public FileController(@Value("${storage.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
    }

    @GetMapping("/api/v1/files/**")
    public ResponseEntity<FileSystemResource> getFile(HttpServletRequest request) {
        String key = request.getRequestURI().substring("/api/v1/files/".length());
        Path filePath = uploadDir.resolve(key).normalize();

        if (!filePath.startsWith(uploadDir)) {
            return ResponseEntity.notFound().build();
        }

        if (!Files.exists(filePath)) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(filePath));
    }
}

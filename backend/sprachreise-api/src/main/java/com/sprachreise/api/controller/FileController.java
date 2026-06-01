package com.sprachreise.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${storage.upload-dir}")
    private String storageDir;

    @GetMapping("/courses/pdf/{filename}")
    public ResponseEntity<Resource> getCoursePdf(@PathVariable String filename) {
        return serveFile("courses/pdf/" + filename, "application/pdf", true);
    }

    /** Streaming vidéo avec support Range (nécessaire pour expo-av / react-native-video) */
    @GetMapping("/courses/video/{*relativePath}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String relativePath,
            HttpServletRequest request) {
        return serveFile(relativePath, "video/mp4", true);
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> getFile(@RequestParam(defaultValue = "") String path) {
        return serveFile(path, MediaType.APPLICATION_OCTET_STREAM_VALUE, false);
    }

    private ResponseEntity<Resource> serveFile(String relativePath, String contentType, boolean inline) {
        try {
            Path filePath = Paths.get(storageDir).resolve(relativePath).normalize();
            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            String disposition = inline ? "inline" : "attachment";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

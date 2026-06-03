package com.sprachreise.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${storage.upload-dir}")
    private String storageDir;

    @GetMapping("/courses/pdf/{filename}")
    public ResponseEntity<Resource> getCoursePdf(@PathVariable String filename) {
        return serveFile("courses/pdf/" + filename, "application/pdf", true);
    }

    @GetMapping("/qcms/results/{filename}")
    public ResponseEntity<Resource> getQcmResultsPdf(@PathVariable String filename) {
        return serveFile("qcms/results/" + filename, "application/pdf", true);
    }

    @GetMapping("/courses/videos/{filename}")
    public ResponseEntity<Resource> getCourseVideo(@PathVariable String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot + 1).toLowerCase();
        String contentType = switch (ext) {
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
        // Spring serves FileSystemResource with range support automatically.
        return serveFile("courses/videos/" + filename, contentType, true);
    }

    @GetMapping("/diplomas/{filename}")
    public ResponseEntity<Resource> getDiploma(@PathVariable String filename) {
        return serveFile("diplomas/" + filename, "application/pdf", true);
    }

    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot + 1).toLowerCase();
        String contentType = switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
        return serveFile("photos/" + filename, contentType, true);
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

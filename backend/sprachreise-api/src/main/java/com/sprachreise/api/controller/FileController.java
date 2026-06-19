package com.sprachreise.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${storage.upload-dir}")
    private String storageDir;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String contentType = file.getContentType() != null ? file.getContentType() : "";

            String subDir = contentType.startsWith("video/") ? "courses/video" : "courses/pdf";
            String storedName = UUID.randomUUID() + ext;

            Path destDir = Paths.get(storageDir).resolve(subDir);
            Files.createDirectories(destDir);
            file.transferTo(destDir.resolve(storedName).toFile());

            String relativePath = subDir + "/" + storedName;
            return ResponseEntity.ok(Map.of("path", relativePath, "name", storedName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload échoué : " + e.getMessage()));
        }
    }

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
    public ResponseEntity<Resource> getFile(HttpServletRequest request) {
        // Extraire le chemin relatif depuis l'URL : /api/files/messages/images/xxx.jpg → messages/images/xxx.jpg
        String requestUri  = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relative    = requestUri.replace(contextPath + "/api/files/", "");
        if (relative.isBlank()) return ResponseEntity.notFound().build();

        // Détecter le content-type depuis l'extension
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = relative.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (lower.endsWith(".png"))  contentType = "image/png";
        else if (lower.endsWith(".gif"))  contentType = "image/gif";
        else if (lower.endsWith(".webp")) contentType = "image/webp";
        else if (lower.endsWith(".mp4"))  contentType = "video/mp4";
        else if (lower.endsWith(".mov"))  contentType = "video/quicktime";
        else if (lower.endsWith(".m4a") || lower.endsWith(".aac")) contentType = "audio/mp4";
        else if (lower.endsWith(".mp3"))  contentType = "audio/mpeg";
        else if (lower.endsWith(".wav"))  contentType = "audio/wav";
        else if (lower.endsWith(".pdf"))  contentType = "application/pdf";

        boolean inline = contentType.startsWith("image/") || contentType.startsWith("audio/") || contentType.startsWith("video/");
        return serveFile(relative, contentType, inline);
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

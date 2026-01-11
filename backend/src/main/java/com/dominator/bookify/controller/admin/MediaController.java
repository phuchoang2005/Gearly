// src/main/java/com/dominator/bookify/controller/MediaController.java
package com.dominator.bookify.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
public class MediaController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // ensure upload directory exists
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // generate a random filename + original extension
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(fn -> fn.contains("."))
                .map(fn -> fn.substring(fn.lastIndexOf('.')))
                .orElse("");
        String filename = UUID.randomUUID() + ext;

        // save to disk
        Path target = Paths.get(UPLOAD_DIR).resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // return the URL you’ll use on the front end
        String url = "/uploads/" + filename;
        return ResponseEntity.ok(Map.of("url", url));
    }
}

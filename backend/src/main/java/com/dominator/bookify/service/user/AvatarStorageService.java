package com.dominator.bookify.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * Stores user avatar files. Writes to a configured uploads directory (served
 * statically at the matching public base path) instead of into the frontend's
 * public folder, so the backend owns its own upload storage.
 */
@Service
public class AvatarStorageService {

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.avatar.public-base:/uploads/avatars}")
    private String publicBase;

    /**
     * Persist {@code file} as {@code {userId}.jpg} and return the public URL path
     * under which it is served.
     */
    public String store(String userId, MultipartFile file) throws IOException {
        String filename = userId + ".jpg";
        File directory = new File(uploadDir).getAbsoluteFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File dest = new File(directory, filename);
        file.transferTo(dest);
        return publicBase + "/" + filename;
    }
}

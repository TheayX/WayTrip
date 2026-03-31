package com.travel.service.support.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 本地文件存储服务，统一负责目录创建和落盘。
 */
@Component
@RequiredArgsConstructor
public class LocalFileStorageService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private final FileNameGenerator fileNameGenerator;
    private final FileValidator fileValidator;

    public StoredFile store(MultipartFile file, String directory, String prefix, String tag) throws IOException {
        String extension = fileValidator.getExtension(file.getOriginalFilename());
        String filename = fileNameGenerator.generate(prefix, tag, extension);

        File uploadDir = new File(uploadPath, directory);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File destFile = new File(uploadDir, filename);
        file.transferTo(destFile.getAbsoluteFile());

        return StoredFile.builder()
            .filename(filename)
            .url("/uploads/" + directory + "/" + filename)
            .absolutePath(destFile.getAbsolutePath())
            .build();
    }
}

package com.travel.service.support.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 本地文件存储服务，统一负责目录创建和落盘。
 * <p>
 * 文件真正写盘的细节集中在这里，上传业务服务只需要关心业务目录和命名策略。
 */
@Component
@RequiredArgsConstructor
public class LocalFileStorageService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private final StorageFileNameGenerator storageFileNameGenerator;
    private final ImageUploadValidator imageUploadValidator;

    /**
     * 将文件落盘到本地目录。
     *
     * @param file 上传文件
     * @param directory 目标子目录
     * @param prefix 文件名前缀
     * @return 落盘结果
     * @throws IOException 文件写入异常
     */
    public StoredFileInfo store(MultipartFile file, String directory, String prefix) throws IOException {
        String extension = imageUploadValidator.getExtension(file.getOriginalFilename());
        String filename = storageFileNameGenerator.generate(prefix, extension);

        File uploadDir = new File(uploadPath, directory);
        if (!uploadDir.exists()) {
            // 目录按需创建，避免部署时预先手工维护所有业务子目录。
            uploadDir.mkdirs();
        }

        File destFile = new File(uploadDir, filename);
        file.transferTo(destFile.getAbsoluteFile());

        return StoredFileInfo.builder()
            .filename(filename)
            .url("/uploads/" + directory + "/" + filename)
            .absolutePath(destFile.getAbsolutePath())
            .build();
    }
}

package com.example.demo.service;

import com.example.demo.dto.ImageUploadResponse;
import com.example.demo.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务 —— 将菜品图片存储到本地磁盘，返回可访问的URL
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final Path uploadDir;
    private final List<String> allowedTypes;

    public FileUploadService(@Value("${file.upload.dir:uploads}") String uploadDirPath,
                             @Value("${file.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}") String allowedTypesStr) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.allowedTypes = Arrays.asList(allowedTypesStr.split(","));
        initUploadDir();
    }

    /**
     * 确保上传目录存在
     */
    private void initUploadDir() {
        try {
            Files.createDirectories(uploadDir);
            log.info("上传目录已就绪：{}", uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败：{}", uploadDir, e);
            throw new BusinessException(500, "服务器文件存储初始化失败");
        }
    }

    /**
     * 上传图片
     *
     * @param file 上传的文件
     * @return 上传结果（包含可访问的URL）
     */
    public ImageUploadResponse uploadImage(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(400, "不支持的图片类型，仅允许：" + String.join(", ", allowedTypes));
        }

        // 生成唯一文件名（保留原始扩展名）
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String savedName = UUID.randomUUID().toString() + extension;

        // 保存文件到磁盘
        try {
            Path targetPath = uploadDir.resolve(savedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("图片上传成功：{} -> {}", originalName, savedName);
        } catch (IOException e) {
            log.error("图片保存失败：{}", savedName, e);
            throw new BusinessException(500, "图片保存失败，请稍后重试");
        }

        // 构建访问URL
        String url = "/uploads/" + savedName;

        return new ImageUploadResponse(url, originalName, savedName, file.getSize());
    }

    /**
     * 删除图片
     *
     * @param filename 文件名
     */
    public void deleteImage(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            // 防止路径遍历攻击
            if (!filePath.startsWith(uploadDir)) {
                throw new BusinessException(400, "非法的文件路径");
            }
            Files.deleteIfExists(filePath);
            log.info("图片已删除：{}", filename);
        } catch (IOException e) {
            log.warn("图片删除失败：{}", filename, e);
        }
    }
}

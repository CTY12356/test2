package com.health.management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.root}")
    private String uploadRoot;

    @Value("${app.upload.public-prefix}")
    private String publicPrefix;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public Map<String, String> store(MultipartFile file) throws IOException {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 图片");
        }

        LocalDate now = LocalDate.now();
        Path directory = Path.of(uploadRoot, String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
        Files.createDirectories(directory);

        String filename = UUID.randomUUID() + "." + extension;
        Path target = directory.resolve(filename);
        file.transferTo(target);

        String url = contextPath + publicPrefix + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + filename;
        return Map.of("url", url);
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new IllegalArgumentException("文件类型不正确");
        }
        return filename.substring(index + 1).toLowerCase();
    }

    /**
     * 将对外访问地址（如 /api/files/2026/05/xxx.jpg）解析为本地文件并读取字节。
     */
    public byte[] readPublicImageBytes(String publicUrl) throws IOException {
        if (!StringUtils.hasText(publicUrl)) {
            throw new IllegalArgumentException("图片地址不能为空");
        }
        String prefix = contextPath + publicPrefix + "/";
        if (!publicUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("仅支持本站上传的图片地址");
        }
        String relative = publicUrl.substring(prefix.length());
        Path root = Path.of(uploadRoot).toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("非法路径");
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("图片文件不存在");
        }
        return Files.readAllBytes(file);
    }

    public String guessMimeTypeForPublicUrl(String publicUrl) {
        String ext = getExtension(publicUrl.contains("/") ? publicUrl.substring(publicUrl.lastIndexOf('/') + 1) : publicUrl);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}

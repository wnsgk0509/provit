package com.provit.service.document.storage.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.provit.service.document.storage.PortfolioFileStorage;

@Component
public class LocalPortfolioFileStorage implements PortfolioFileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalPortfolioFileStorage.class);

    private final Path storageDirectory;

    public LocalPortfolioFileStorage(
            @Value("${portfolio.upload.path:D:/fileStorage_Provit/portfolio_uploadfile}") String uploadPath) {
        this.storageDirectory = Path.of(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public String store(int portfolioNum, MultipartFile file) {
        Path targetPath = storageDirectory.resolve(portfolioNum + ".pdf").normalize();
        if (!storageDirectory.equals(targetPath.getParent())) {
            throw new IllegalArgumentException("포트폴리오 저장 경로가 올바르지 않습니다.");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetPath.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException("포트폴리오 파일을 저장하지 못했습니다.", exception);
        }
    }

    @Override
    public void deleteIfExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(fileUrl));
        } catch (IOException exception) {
            log.error("롤백된 포트폴리오 파일을 삭제하지 못했습니다: {}", fileUrl, exception);
        }
    }

    @Override
    public Resource loadAsResource(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalStateException("등록된 포트폴리오 파일이 없습니다.");
        }

        Path filePath = Path.of(fileUrl).toAbsolutePath().normalize();
        if (!storageDirectory.equals(filePath.getParent()) || !Files.isRegularFile(filePath)) {
            throw new IllegalStateException("포트폴리오 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(filePath);
    }
}

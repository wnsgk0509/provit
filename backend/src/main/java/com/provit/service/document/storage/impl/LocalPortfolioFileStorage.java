package com.provit.service.document.storage.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.FileAlreadyExistsException;
import java.util.UUID;

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
    private final Path legacyStorageDirectory;

    public LocalPortfolioFileStorage(
            @Value("${portfolio.upload.path:D:/privateFileStorage_Provit/portfolio_uploadfile}") String uploadPath,
            @Value("${file.upload.base-dir:D:/fileStorage_Provit}") String publicUploadPath) {
        this.storageDirectory = Path.of(uploadPath).toAbsolutePath().normalize();
        Path publicDirectory = Path.of(publicUploadPath).toAbsolutePath().normalize();
        if (storageDirectory.startsWith(publicDirectory)) {
            throw new IllegalArgumentException("포트폴리오 저장 위치는 공개 업로드 디렉토리 밖이어야 합니다.");
        }
        this.legacyStorageDirectory = publicDirectory.resolve("portfolio_uploadfile");
    }

    @Override
    public String store(int portfolioNum, MultipartFile file) {
        Path targetPath = storageDirectory.resolve(UUID.randomUUID() + ".pdf");

        try {
            Files.createDirectories(storageDirectory);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }
            return targetPath.toString();
        } catch (IOException exception) {
            if (exception instanceof FileAlreadyExistsException) {
                throw new UncheckedIOException("포트폴리오 파일을 저장하지 못했습니다.", exception);
            }
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new UncheckedIOException("포트폴리오 파일을 저장하지 못했습니다.", exception);
        }
    }

    @Override
    public void deleteIfExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(resolveFilePath(fileUrl));
        } catch (IOException exception) {
            log.error("롤백된 포트폴리오 파일을 삭제하지 못했습니다: {}", fileUrl, exception);
        }
    }

    @Override
    public Resource loadAsResource(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalStateException("등록된 포트폴리오 파일이 없습니다.");
        }

        Path filePath = resolveFilePath(fileUrl);
        if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("포트폴리오 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(filePath);
    }

    private Path resolveFilePath(String fileUrl) {
        Path filePath = Path.of(fileUrl).toAbsolutePath().normalize();
        if ((!storageDirectory.equals(filePath.getParent())
                && !legacyStorageDirectory.equals(filePath.getParent()))
                || Files.isSymbolicLink(filePath)) {
            throw new IllegalStateException("포트폴리오 저장 경로가 올바르지 않습니다.");
        }
        return filePath;
    }
}

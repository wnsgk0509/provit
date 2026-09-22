package com.provit.service.file.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.provit.common.file.FileCategory;
import com.provit.dto.file.FileUploadResponseDTO;
import com.provit.service.file.FileUploadService;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    /**
     * 파일 저장 최상위 공통 루트 디렉토리
     * 프로퍼티에 설정이 없으면 기본값 'D:/fileStorage_Provit'을 사용합니다.
     */
    @Value("${file.upload.base-dir:D:/fileStorage_Provit}")
    private String baseUploadDir;

    /**
     * 스프링 빈 생성 후 각 카테고리별 디렉토리가 없으면 자동으로 생성합니다.
     */
    @PostConstruct
    public void initDirectories() {
        try {
            Path basePath = Paths.get(baseUploadDir);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info(">> [FileUpload] 공통 루트 저장 디렉토리 생성 완료: {}", basePath.toAbsolutePath());
            }

            for (FileCategory category : FileCategory.values()) {
                Path categoryPath = basePath.resolve(category.getSubDirectory());
                if (!Files.exists(categoryPath)) {
                    Files.createDirectories(categoryPath);
                    log.info(">> [FileUpload] 서브 카테고리 디렉토리 생성 완료: {}", categoryPath.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            log.error(">> [FileUpload] 초기 업로드 디렉토리 생성 실패: {}", e.getMessage(), e);
        }
    }

    @Override
    public FileUploadResponseDTO uploadFile(MultipartFile file, FileCategory category, long targetId) {
        // 1. 고유 NUM 및 카테고리 유효성 검사
        if (targetId <= 0) {
            throw new IllegalArgumentException("파일과 연계할 대상 DB의 고유 번호(num)가 필요합니다. (전달된 값: " + targetId + ")");
        }
        category.validate(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unnamed_file";
        }

        // 파일명 내 특수문자 및 공백 정제
        String cleanedOriginalFilename = Paths.get(originalFilename).getFileName().toString().replaceAll("\\s+", "_");

        // 2. DB 고유 NUM 기반 파일명 생성 (예: 10_이력서.pdf, 42_스크린샷.png)
        String savedFileName = targetId + "_" + cleanedOriginalFilename;

        // 3. 대상 저장 경로 생성 (예: D:\fileStorage_Provit\post_uploadfile\42_스크린샷.png)
        Path targetDirPath = Paths.get(baseUploadDir, category.getSubDirectory());
        Path targetFilePath = targetDirPath.resolve(savedFileName);

        try {
            if (!Files.exists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            // 파일 스트림 복사 저장
            Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info(">> [FileUpload] 파일 저장 성공: 카테고리={}, 고유NUM={}, 원본명={}, 저장명={}, 크기={} bytes",
                    category.name(), targetId, originalFilename, savedFileName, file.getSize());

            // 4. 클라이언트 웹 접근 URL 생성 (예: /uploads/post_uploadfile/42_스크린샷.png)
            String fileUrl = "/uploads/" + category.getSubDirectory() + "/" + savedFileName;

            return FileUploadResponseDTO.builder()
                    .targetId(targetId)
                    .originalFileName(originalFilename)
                    .savedFileName(savedFileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .category(category.name())
                    .uploadedAt(new Date())
                    .build();

        } catch (IOException e) {
            log.error(">> [FileUpload] 파일 디스크 저장 중 I/O 에러 발생: {}", e.getMessage(), e);
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(FileCategory category, String savedFileName) {
        if (savedFileName == null || savedFileName.isBlank()) {
            return false;
        }

        try {
            // 경로 조작 공격(Path Traversal: ../) 방지
            String sanitizedFilename = Paths.get(savedFileName).getFileName().toString();
            Path filePath = Paths.get(baseUploadDir, category.getSubDirectory(), sanitizedFilename);

            File targetFile = filePath.toFile();
            if (targetFile.exists() && targetFile.isFile()) {
                boolean deleted = targetFile.delete();
                log.info(">> [FileUpload] 파일 삭제 {}: 경로={}", deleted ? "성공" : "실패", filePath.toAbsolutePath());
                return deleted;
            } else {
                log.warn(">> [FileUpload] 삭제할 대상 파일이 존재하지 않습니다: {}", filePath.toAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            log.error(">> [FileUpload] 파일 삭제 처리 중 오류: {}", e.getMessage(), e);
            return false;
        }
    }
}

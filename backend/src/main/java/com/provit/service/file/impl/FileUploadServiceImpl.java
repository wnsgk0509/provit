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

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.provit.common.file.FileCategory;
import com.provit.dto.file.FileUploadResponseDTO;
import com.provit.service.file.FileUploadService;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    private final SqlSessionTemplate sqlSessionTemplate;

    /**
     * 파일 저장 최상위 공통 루트 디렉토리
     * 프로퍼티에 설정이 없으면 기본값 'D:/fileStorage_Provit'을 사용합니다.
     */
    @Value("${file.upload.base-dir:D:/fileStorage_Provit}")
    private String baseUploadDir;

    @Autowired
    public FileUploadServiceImpl(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

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
    public FileUploadResponseDTO uploadFile(
            MultipartFile file, FileCategory category, long targetId, long userNum, String userRole) {

        // 1. 고유 NUM 및 카테고리 유효성 검사
        if (targetId <= 0) {
            throw new IllegalArgumentException("파일과 연계할 대상 DB의 고유 번호(targetId)가 필요합니다. (전달된 값: " + targetId + ")");
        }
        category.validate(file);

        // 2. 리소스 소유권 및 권한 검증 (BOLA / IDOR 방지)
        validateResourceOwnership(category, targetId, userNum, userRole);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unnamed_file";
        }

        // 파일명 내 특수문자 및 공백 정제
        String cleanedOriginalFilename = Paths.get(originalFilename).getFileName().toString().replaceAll("\\s+", "_");

        // 3. 고유 NUM + 8자리 난수 결합 파일명 생성 (중복 덮어쓰기 방지 및 URL 추측 공격 원천 차단)
        // 예: 10_a8f3b2c1_이력서.pdf, 42_9b3d7a12_스크린샷.png
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String savedFileName = targetId + "_" + randomSuffix + "_" + cleanedOriginalFilename;

        // 4. 대상 저장 경로 생성 (예: D:\fileStorage_Provit\post_uploadfile\42_9b3d7a12_스크린샷.png)
        Path targetDirPath = Paths.get(baseUploadDir, category.getSubDirectory());
        Path targetFilePath = targetDirPath.resolve(savedFileName);

        try {
            if (!Files.exists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            // 파일 스트림 복사 저장
            Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info(">> [FileUpload] 파일 저장 성공: 카테고리={}, 고유NUM={}, 요청자={}, 원본명={}, 저장명={}, 크기={} bytes",
                    category.name(), targetId, userNum, originalFilename, savedFileName, file.getSize());

            // 5. 클라이언트 웹 접근 URL 생성 (예: /uploads/post_uploadfile/42_9b3d7a12_스크린샷.png)
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
    public boolean deleteFile(FileCategory category, String savedFileName, long userNum, String userRole) {
        if (savedFileName == null || savedFileName.isBlank()) {
            return false;
        }

        try {
            // 경로 조작 공격(Path Traversal: ../) 방지
            String sanitizedFilename = Paths.get(savedFileName).getFileName().toString();

            // 1. 파일명에서 targetId 추출하여 소유권 검증 (파일명 형식: {targetId}_{난수}_{원본파일명})
            long targetId = parseTargetIdFromFileName(sanitizedFilename);
            validateResourceOwnership(category, targetId, userNum, userRole);

            Path filePath = Paths.get(baseUploadDir, category.getSubDirectory(), sanitizedFilename);
            File targetFile = filePath.toFile();

            if (targetFile.exists() && targetFile.isFile()) {
                boolean deleted = targetFile.delete();
                log.info(">> [FileUpload] 파일 삭제 {}: 카테고리={}, 요청자={}, 파일={}",
                        deleted ? "성공" : "실패", category.name(), userNum, filePath.toAbsolutePath());
                return deleted;
            } else {
                log.warn(">> [FileUpload] 삭제할 대상 파일이 존재하지 않습니다: {}", filePath.toAbsolutePath());
                return false;
            }
        } catch (SecurityException e) {
            log.warn(">> [FileUpload] 파일 삭제 권한 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(">> [FileUpload] 파일 삭제 처리 중 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 카테고리별 대상 리소스에 대한 DB 존재 여부 및 요청자의 소유권/권한을 엄격히 검증합니다.
     * 1단계: DB 상에 해당 대상 엔티티(포트폴리오, 게시글, 회원)가 실제로 존재하는지 확인 (Existence Check)
     * 2단계: 조회된 리소스의 실제 소유자(USER_NUM)와 요청자(userNum)가 일치하는지 대조 (Ownership Check)
     */
    private void validateResourceOwnership(FileCategory category, long targetId, long userNum, String role) {
        // 관리자(ADMIN)는 모든 파일 업로드/삭제 가능
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        switch (category) {
            case PROFILE -> {
                // 1. 본인의 userNum으로만 프로필 업로드/삭제 가능
                if (targetId != userNum) {
                    throw new SecurityException("본인의 프로필 사진만 업로드하거나 삭제할 수 있습니다. (요청 회원: " + userNum + ", 대상: " + targetId + ")");
                }
                // 2. 실제 유효한(미탈퇴) 회원인지 DB 검증
                Integer count = sqlSessionTemplate.selectOne(
                        "com.provit.mapper.file.FileMapper.selectUserExists", targetId);
                if (count == null || count <= 0) {
                    throw new IllegalArgumentException("존재하지 않거나 탈퇴 처리된 회원입니다: " + targetId);
                }
            }
            case PORTFOLIO -> {
                // 1. DB에서 해당 포트폴리오 존재 여부 및 소유자 조회
                Integer ownerNum = sqlSessionTemplate.selectOne(
                        "com.provit.mapper.file.FileMapper.selectPortfolioOwner", targetId);
                if (ownerNum == null) {
                    throw new IllegalArgumentException("존재하지 않는 포트폴리오 번호입니다: " + targetId);
                }
                // 2. 실제 소유자 일치 검증
                if (ownerNum.intValue() != (int) userNum) {
                    throw new SecurityException("본인이 등록한 포트폴리오의 파일만 업로드하거나 삭제할 수 있습니다.");
                }
            }
            case POST -> {
                // 1. DB에서 해당 게시글 존재 여부 및 작성자 조회
                Integer writerNum = sqlSessionTemplate.selectOne(
                        "com.provit.mapper.file.FileMapper.selectPostOwner", targetId);
                if (writerNum == null) {
                    throw new IllegalArgumentException("존재하지 않는 게시글 번호입니다: " + targetId);
                }
                // 2. 실제 작성자 일치 검증
                if (writerNum.intValue() != (int) userNum) {
                    throw new SecurityException("본인이 작성한 게시글의 파일만 업로드하거나 삭제할 수 있습니다.");
                }
            }
        }
    }

    /**
     * 저장 파일명({targetId}_{난수}_{원본파일명})에서 targetId를 파싱합니다.
     */
    private long parseTargetIdFromFileName(String savedFileName) {
        int underscoreIdx = savedFileName.indexOf('_');
        if (underscoreIdx <= 0) {
            throw new IllegalArgumentException("올바른 형식의 파일명이 아닙니다. (" + savedFileName + ")");
        }

        try {
            return Long.parseLong(savedFileName.substring(0, underscoreIdx));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("파일명에서 고유 식별 번호(targetId)를 추출할 수 없습니다: " + savedFileName);
        }
    }
}

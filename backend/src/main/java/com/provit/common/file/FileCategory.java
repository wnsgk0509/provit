package com.provit.common.file;

import java.util.Arrays;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;

/**
 * 파일 업로드 목적별 카테고리 Enum
 * - 목적별 서브 디렉토리, 허용 확장자(화이트리스트), 최대 업로드 용량을 중앙에서 정의/관리합니다.
 */
@Getter
public enum FileCategory {

    /**
     * 포트폴리오 (PDF 전용, 최대 20MB)
     * 저장 경로: D:\fileStorage_Provit\portfolio_uploadfile
     */
    PORTFOLIO("portfolio_uploadfile", Set.of("pdf"), 20 * 1024 * 1024L, "PDF 파일만 업로드 가능합니다."),

    /**
     * 게시글 첨부파일 (이미지 전용, 최대 10MB)
     * 저장 경로: D:\fileStorage_Provit\post_uploadfile
     */
    POST("post_uploadfile", Set.of("jpg", "jpeg", "png", "gif", "webp"), 10 * 1024 * 1024L, "이미지 파일(jpg, jpeg, png, gif, webp)만 업로드 가능합니다."),

    /**
     * 회원 프로필 사진 (이미지 전용, 최대 5MB)
     * 저장 경로: D:\fileStorage_Provit\profile_uploadfile
     */
    PROFILE("profile_uploadfile", Set.of("jpg", "jpeg", "png", "gif", "webp"), 5 * 1024 * 1024L, "이미지 파일(jpg, jpeg, png, gif, webp)만 업로드 가능합니다.");

    private final String subDirectory;
    private final Set<String> allowedExtensions;
    private final long maxSizeBytes;
    private final String formatErrorMessage;

    FileCategory(String subDirectory, Set<String> allowedExtensions, long maxSizeBytes, String formatErrorMessage) {
        this.subDirectory = subDirectory;
        this.allowedExtensions = allowedExtensions;
        this.maxSizeBytes = maxSizeBytes;
        this.formatErrorMessage = formatErrorMessage;
    }

    /**
     * 문자열 카테고리명을 Enum으로 변환 (대소문자 무관 및 별칭 지원)
     */
    public static FileCategory fromString(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) {
            throw new IllegalArgumentException("업로드 카테고리가 지정되지 않았습니다.");
        }

        String normalized = categoryStr.trim().toUpperCase();
        for (FileCategory cat : values()) {
            if (cat.name().equals(normalized) || cat.getSubDirectory().equalsIgnoreCase(categoryStr.trim())) {
                return cat;
            }
        }

        throw new IllegalArgumentException("지원하지 않는 업로드 카테고리입니다: " + categoryStr
                + " (사용 가능: " + Arrays.toString(values()) + ")");
    }

    /**
     * 업로드된 파일의 유효성(크기 및 확장자)을 검증합니다.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        // 1. 파일 용량 검증
        if (file.getSize() > this.maxSizeBytes) {
            long maxMb = this.maxSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException("파일 용량이 허용치를 초과했습니다. (최대 " + maxMb + "MB)");
        }

        // 2. 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("확장자가 없는 파일은 업로드할 수 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!this.allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(this.formatErrorMessage + " (요청된 파일: ." + extension + ")");
        }
    }
}

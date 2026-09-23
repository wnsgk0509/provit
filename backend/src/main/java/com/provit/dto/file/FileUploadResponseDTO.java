package com.provit.dto.file;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 파일 업로드 완료 후 클라이언트에게 반환하는 공통 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDTO {

    /** 대상 엔티티 고유 번호 (portfolioNum, postNum, userNum 등) */
    private long targetId;

    /** 원본 파일명 (예: "내_포트폴리오.pdf") */
    private String originalFileName;

    /** 디스크에 저장된 고유 파일명 (예: "a1b2c3d4-e5f6-..._내_포트폴리오.pdf") */
    private String savedFileName;

    /** 브라우저에서 접근 가능한 웹 URL (예: "/uploads/portfolio_uploadfile/a1b2c3d4-..._내_포트폴리오.pdf") */
    private String fileUrl;

    /** 파일 크기 (bytes) */
    private long fileSize;

    /** 파일 업로드 목적 카테고리 (PORTFOLIO, POST, PROFILE) */
    private String category;

    /** 업로드 완료 일시 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date uploadedAt;
}

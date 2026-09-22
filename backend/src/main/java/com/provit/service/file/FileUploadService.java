package com.provit.service.file;

import org.springframework.web.multipart.MultipartFile;

import com.provit.common.file.FileCategory;
import com.provit.dto.file.FileUploadResponseDTO;

public interface FileUploadService {

    /**
     * 지정된 카테고리의 서브 디렉토리에 고유 NUM 값을 기반으로 파일명을 생성하여 저장합니다.
     *
     * @param file     업로드할 멀티파트 파일
     * @param category 업로드 목적 카테고리 (PORTFOLIO, POST, PROFILE)
     * @param targetId 대상 DB 엔티티 고유 번호 (portfolioNum, postNum, userNum 등)
     * @return 저장된 파일의 메타데이터 및 웹 접근 URL
     */
    FileUploadResponseDTO uploadFile(MultipartFile file, FileCategory category, long targetId);

    /**
     * 저장된 파일을 물리 디스크에서 삭제합니다.
     *
     * @param category      소속 카테고리
     * @param savedFileName 삭제할 저장 파일명
     * @return 삭제 성공 여부
     */
    boolean deleteFile(FileCategory category, String savedFileName);
}

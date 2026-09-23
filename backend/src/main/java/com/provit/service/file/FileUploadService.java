package com.provit.service.file;

import org.springframework.web.multipart.MultipartFile;

import com.provit.common.file.FileCategory;
import com.provit.dto.file.FileUploadResponseDTO;

public interface FileUploadService {

    /**
     * 지정된 카테고리의 서브 디렉토리에 고유 NUM 및 난수를 결합하여 안전하게 파일을 저장합니다.
     * 호출자의 소유권 및 접근 권한을 검증합니다.
     *
     * @param file     업로드할 멀티파트 파일
     * @param category 업로드 목적 카테고리 (PORTFOLIO, POST, PROFILE)
     * @param targetId 대상 DB 엔티티 고유 번호 (portfolioNum, postNum, userNum 등)
     * @param userNum  요청자 회원 번호 (JWT 인증)
     * @param userRole 요청자 권한 (USER, ADMIN)
     * @return 저장된 파일의 메타데이터 및 웹 접근 URL
     */
    FileUploadResponseDTO uploadFile(MultipartFile file, FileCategory category, long targetId, long userNum, String userRole);

    /**
     * 저장된 파일을 물리 디스크에서 안전하게 삭제합니다.
     * 파일 소유자 또는 관리자(ADMIN) 권한을 검증합니다.
     *
     * @param category      소속 카테고리
     * @param savedFileName 삭제할 저장 파일명
     * @param userNum       요청자 회원 번호 (JWT 인증)
     * @param userRole      요청자 권한 (USER, ADMIN)
     * @return 삭제 성공 여부
     */
    boolean deleteFile(FileCategory category, String savedFileName, long userNum, String userRole);
}

package com.provit.controller.file;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.provit.common.ResponseCode;
import com.provit.common.file.FileCategory;
import com.provit.dto.file.FileUploadResponseDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.file.FileUploadService;
import com.provit.util.jwt.JwtProvider;

/**
 * 공통 파일 업로드/삭제 REST API 컨트롤러
 * 
 * [엔드포인트 규격]
 * - POST /api/upload/portfolio?targetId=15 : 포트폴리오 PDF 파일 업로드 (.pdf 전용, 최대 20MB, 파일명: 15_원본파일명.pdf)
 * - POST /api/upload/post?targetId=102     : 커뮤니티 게시글 이미지 업로드 (.jpg, .png 등, 최대 10MB, 파일명: 102_원본파일명.png)
 * - POST /api/upload/profile?targetId=3    : 회원 프로필 사진 업로드 (.jpg, .png 등, 최대 5MB, 파일명: 3_원본파일명.jpg)
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;
    private final JwtProvider jwtProvider;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService, JwtProvider jwtProvider) {
        this.fileUploadService = fileUploadService;
        this.jwtProvider = jwtProvider;
    }

    /**
     * 카테고리별 단일 파일 업로드 API
     * DB 테이블의 고유 NUM 값을 전달받아 파일명(예: {targetId}_{원본파일명})으로 명명합니다.
     */
    @PostMapping("/{category}")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadFile(
            HttpServletRequest request,
            @PathVariable("category") String categoryName,
            @RequestParam(value = "targetId", required = false) Long targetId,
            @RequestParam("file") MultipartFile file) {

        log.info(">> [/api/upload/{}] 파일 업로드 요청 수신: targetId={}, 파일명={}, 크기={} bytes",
                categoryName, targetId, file != null ? file.getOriginalFilename() : "null", file != null ? file.getSize() : 0);

        try {
            // 1. 카테고리 Enum 변환
            FileCategory category = FileCategory.fromString(categoryName);

            // 2. targetId 누락 시 프로필의 경우 로그인 JWT 토큰에서 자동 추출 시도
            if (targetId == null || targetId <= 0) {
                if (category == FileCategory.PROFILE) {
                    Long authUserNum = getAuthenticatedUserNum(request);
                    if (authUserNum != null && authUserNum > 0) {
                        targetId = authUserNum;
                    }
                }
            }

            if (targetId == null || targetId <= 0) {
                throw new IllegalArgumentException("파일과 연계할 DB 테이블의 고유 번호(targetId)를 전달해 주세요. (예: portfolioNum, postNum, userNum)");
            }

            // 3. 파일 검증 및 DB 고유 NUM 기반 저장 수행
            FileUploadResponseDTO responseDTO = fileUploadService.uploadFile(file, category, targetId);

            return ResponseEntity.ok(ApiResponse.success(responseDTO));

        } catch (IllegalArgumentException e) {
            log.warn(">> [/api/upload/{}] 파일 검증 실패: {}", categoryName, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, null));
        } catch (Exception e) {
            log.error(">> [/api/upload/{}] 서버 오류 발생: {}", categoryName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR, null));
        }
    }

    private Long getAuthenticatedUserNum(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            return null;
        }
        return jwtProvider.getUserNum(token);
    }

    /**
     * 카테고리별 저장된 파일 삭제 API
     */
    @DeleteMapping("/{category}/{savedFileName}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable("category") String categoryName,
            @PathVariable("savedFileName") String savedFileName) {

        log.info(">> [/api/upload/{}/{}] 파일 삭제 요청 수신", categoryName, savedFileName);

        try {
            FileCategory category = FileCategory.fromString(categoryName);
            boolean deleted = fileUploadService.deleteFile(category, savedFileName);

            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("파일이 성공적으로 삭제되었습니다."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, "파일을 찾을 수 없거나 삭제할 수 없습니다."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, e.getMessage()));
        }
    }
}

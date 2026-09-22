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
     * DB 테이블의 고유 NUM 값을 전달받아 파일명(예: {targetId}_{난수}_{원본파일명})으로 명명합니다.
     * JWT 인증 및 리소스 소유권을 철저히 검증합니다.
     */
    @PostMapping("/{category}")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadFile(
            HttpServletRequest request,
            @PathVariable("category") String categoryName,
            @RequestParam(value = "targetId", required = false) Long targetId,
            @RequestParam("file") MultipartFile file) {

        // 1. JWT 인증 검증 (비로그인 사용자 차단)
        String token = extractToken(request);
        if (token == null) {
            log.warn(">> [/api/upload/{}] 인증되지 않은 사용자의 업로드 시도 차단", categoryName);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null));
        }
        Long authUserNum = jwtProvider.getUserNum(token);
        String authRole = jwtProvider.getUserRole(token);

        log.info(">> [/api/upload/{}] 파일 업로드 요청 수신: authUserNum={}, role={}, targetId={}, 파일명={}, 크기={} bytes",
                categoryName, authUserNum, authRole, targetId, file != null ? file.getOriginalFilename() : "null", file != null ? file.getSize() : 0);

        try {
            // 2. 카테고리 Enum 변환
            FileCategory category = FileCategory.fromString(categoryName);

            // 3. targetId 처리
            // PROFILE: 타인 프로필 변조 방지를 위해 무조건 로그인 사용자의 userNum으로 강제 고정
            if (category == FileCategory.PROFILE) {
                targetId = authUserNum;
            } else {
                // 포트폴리오/게시글 등은 대상 DB 고유 NUM이 반드시 유효해야 함 (임의 우회 원천 차단)
                if (targetId == null || targetId <= 0) {
                    throw new IllegalArgumentException("파일을 연결할 대상 고유 번호(targetId)가 누락되었거나 유효하지 않습니다. (포트폴리오 번호 또는 게시글 번호)");
                }
            }

            // 4. DB 존재 여부 및 리소스 소유권 검증 후 파일 저장 (고유 NUM + 난수 결합 명명)
            FileUploadResponseDTO responseDTO = fileUploadService.uploadFile(
                    file, category, targetId, authUserNum, authRole);

            return ResponseEntity.ok(ApiResponse.success(responseDTO));

        } catch (IllegalArgumentException e) {
            log.warn(">> [/api/upload/{}] 파일 유효성 검증 실패: {}", categoryName, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, null));
        } catch (SecurityException e) {
            log.warn(">> [/api/upload/{}] 리소스 소유권 검증 실패 (BOLA/IDOR 차단): {}", categoryName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null));
        } catch (Exception e) {
            log.error(">> [/api/upload/{}] 파일 업로드 서버 오류 발생: {}", categoryName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR, null));
        }
    }

    /**
     * 카테고리별 저장된 파일 삭제 API
     * JWT 인증 및 본인 파일(또는 관리자) 소유권 검증을 통과해야만 물리 파일이 삭제됩니다.
     */
    @DeleteMapping("/{category}/{savedFileName}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            HttpServletRequest request,
            @PathVariable("category") String categoryName,
            @PathVariable("savedFileName") String savedFileName) {

        // 1. JWT 인증 검증 (비로그인 사용자 차단)
        String token = extractToken(request);
        if (token == null) {
            log.warn(">> [/api/upload/{}/{}] 비인증 사용자의 파일 삭제 시도 차단", categoryName, savedFileName);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null));
        }
        Long authUserNum = jwtProvider.getUserNum(token);
        String authRole = jwtProvider.getUserRole(token);

        log.info(">> [/api/upload/{}/{}] 파일 삭제 요청 수신: authUserNum={}, role={}",
                categoryName, savedFileName, authUserNum, authRole);

        try {
            FileCategory category = FileCategory.fromString(categoryName);
            boolean deleted = fileUploadService.deleteFile(category, savedFileName, authUserNum, authRole);

            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("파일이 성공적으로 삭제되었습니다."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, "파일을 찾을 수 없거나 삭제할 수 없습니다."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(ResponseCode.BAD_REQUEST, e.getMessage()));
        } catch (SecurityException e) {
            log.warn(">> [/api/upload/{}/{}] 파일 삭제 권한 없음: {}", categoryName, savedFileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, e.getMessage()));
        } catch (Exception e) {
            log.error(">> [/api/upload/{}/{}] 파일 삭제 처리 중 서버 오류: {}", categoryName, savedFileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR, null));
        }
    }

    /**
     * 요청 헤더의 Authorization 토큰을 추출하고 유효성을 검증합니다.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return jwtProvider.validateToken(token) ? token : null;
    }
}

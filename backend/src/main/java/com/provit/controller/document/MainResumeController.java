package com.provit.controller.document;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.dto.document.MainResumeRequestDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.document.MainResumeService;

@RestController
@RequestMapping("/api/documents/main-resume")
public class MainResumeController {
    private final MainResumeService mainResumeService;

    public MainResumeController(MainResumeService mainResumeService) {
        this.mainResumeService = mainResumeService;
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MainResumeJobInfoDTO>> setMainResume(
            @LoginUser Long userNum, @RequestBody MainResumeRequestDTO request) {
        if (userNum == null) return unauthorized();
        if (request == null || request.getResumeNum() == null) {
            throw new IllegalArgumentException("이력서 번호를 입력해 주세요.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(mainResumeService.setMainResume(userNum, request.getResumeNum())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MainResumeJobInfoDTO>> getMainResume(@LoginUser Long userNum) {
        if (userNum == null) return unauthorized();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(mainResumeService.getMainResumeJobInfo(userNum)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearMainResume(@LoginUser Long userNum) {
        if (userNum == null) return unauthorized();
        mainResumeService.clearMainResume(userNum);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> invalidRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(ApiResponse.error(ResponseCode.BAD_REQUEST, "이력서 번호를 정수로 입력해 주세요."));
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                .body(ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED));
    }
}

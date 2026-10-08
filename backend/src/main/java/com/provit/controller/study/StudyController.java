package com.provit.controller.study;

import com.provit.dto.response.ApiResponse;
import com.provit.dto.study.StudyDTO;
import com.provit.service.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.provit.common.annotation.LoginUser;
import com.provit.common.ResponseCode;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyService studyService;

    @Autowired
    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    /**
     * 1. 스터디 전체 목록 조회
     * @param userNum 로그인한 유저의 식별자 (선택) - 본인이 참여 중인지 알기 위함
     */
    @GetMapping
    public ApiResponse<List<StudyDTO>> getStudyList(@LoginUser Long userNum) {
        List<StudyDTO> list = studyService.getStudyList(userNum);
        return ApiResponse.success(list);
    }

    /**
     * 2. 스터디 개설
     */
    @PostMapping
    public ApiResponse<Long> createStudy(@RequestBody StudyDTO studyDto, @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        studyDto.setUserNum(userNum);
        Long studyNum = studyService.createStudy(studyDto);
        return ApiResponse.success(studyNum);
    }

    /**
     * 3. 스터디 삭제 (방장 전용)
     */
    @DeleteMapping("/{studyNum}")
    public ApiResponse<Void> deleteStudy(@PathVariable Long studyNum, @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        // 실제 운영 환경에서는 로그인한 유저 세션/토큰의 userNum과
        // 해당 스터디의 userNum(방장)이 일치하는지 백엔드에서 검증해야 함
        // (StudyService/DAO 에서 WHERE 조건으로 검증)
        studyService.deleteStudy(studyNum, userNum);
        return ApiResponse.success();
    }

    /**
     * 3-1. 스터디 수정 (방장 전용)
     */
    @PutMapping("/{studyNum}")
    public ApiResponse<Void> updateStudy(
            @PathVariable Long studyNum,
            @RequestBody StudyDTO studyDto,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        studyDto.setStudyNum(studyNum);
        studyDto.setUserNum(userNum);
        studyService.updateStudy(studyDto);
        return ApiResponse.success();
    }

    /**
     * 4. 스터디 참여하기
     */
    @PostMapping("/{studyNum}/join")
    public ApiResponse<Void> joinStudy(
            @PathVariable Long studyNum,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        studyService.joinStudy(studyNum, userNum);
        return ApiResponse.success();
    }

    /**
     * 5. 스터디 참여 취소(탈퇴)
     */
    @DeleteMapping("/{studyNum}/leave")
    public ApiResponse<Void> leaveStudy(
            @PathVariable Long studyNum,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        studyService.leaveStudy(studyNum, userNum);
        return ApiResponse.success();
    }

    /**
     * 6. 스터디 참여자 목록 조회
     */
    @GetMapping("/{studyNum}/members")
    public ApiResponse<List<String>> getStudyMembers(@PathVariable Long studyNum) {
        List<String> members = studyService.getStudyMembers(studyNum);
        return ApiResponse.success(members);
    }
}

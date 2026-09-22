package com.provit.controller.study;

import com.provit.dto.response.ApiResponse;
import com.provit.dto.study.StudyDTO;
import com.provit.service.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<List<StudyDTO>> getStudyList(@RequestParam(required = false) Long userNum) {
        List<StudyDTO> list = studyService.getStudyList(userNum);
        return ApiResponse.success(list);
    }

    /**
     * 2. 스터디 개설
     */
    @PostMapping
    public ApiResponse<Long> createStudy(@RequestBody StudyDTO studyDto) {
        Long studyNum = studyService.createStudy(studyDto);
        return ApiResponse.success(studyNum);
    }

    /**
     * 3. 스터디 삭제 (방장 전용)
     */
    @DeleteMapping("/{studyNum}")
    public ApiResponse<Void> deleteStudy(@PathVariable Long studyNum) {
        // 실제 운영 환경에서는 로그인한 유저 세션/토큰의 userNum과
        // 해당 스터디의 userNum(방장)이 일치하는지 백엔드에서 검증해야 함
        studyService.deleteStudy(studyNum);
        return ApiResponse.success();
    }

    /**
     * 3-1. 스터디 수정 (방장 전용)
     */
    @PutMapping("/{studyNum}")
    public ApiResponse<Void> updateStudy(
            @PathVariable Long studyNum,
            @RequestBody StudyDTO studyDto) {
        studyDto.setStudyNum(studyNum);
        studyService.updateStudy(studyDto);
        return ApiResponse.success();
    }

    /**
     * 4. 스터디 참여하기
     */
    @PostMapping("/{studyNum}/join")
    public ApiResponse<Void> joinStudy(
            @PathVariable Long studyNum,
            @RequestBody StudyDTO studyDto) {
        // body에서 userNum만 꺼내서 사용
        studyService.joinStudy(studyNum, studyDto.getUserNum());
        return ApiResponse.success();
    }

    /**
     * 5. 스터디 참여 취소(탈퇴)
     */
    @DeleteMapping("/{studyNum}/leave")
    public ApiResponse<Void> leaveStudy(
            @PathVariable Long studyNum,
            @RequestParam Long userNum) {
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

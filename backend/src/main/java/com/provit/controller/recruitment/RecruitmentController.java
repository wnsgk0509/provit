package com.provit.controller.recruitment;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.recruitment.RecruitmentService;

@RestController
@RequestMapping("/api/occupation")
public class RecruitmentController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentController.class);

    private final RecruitmentService recruitmentService;

    @Autowired
    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping
    public ApiResponse<List<OccupationDTO>> occupation() {
        log.info(">> [/api/occupation] 직군 목록 조회 요청 수신");

        List<OccupationDTO> occupationList = recruitmentService.getSarmainOccupationInfo();

        // 콘솔창 출력 (직접 확인용)
        System.out.println("\n=======================================================");
        System.out.println("  [Controller] 직군 목록 DB 조회 결과 (총 " + (occupationList != null ? occupationList.size() : 0) + "건)");
        System.out.println("=======================================================");
        if (occupationList != null) {
            for (OccupationDTO occ : occupationList) {
                System.out.println("  - 직군코드: " + occ.getOccupationCode() + " | 직군명: " + occ.getOccupationName());
            }
        }
        System.out.println("=======================================================\n");

        return ApiResponse.success(occupationList);
    }
}

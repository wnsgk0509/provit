package com.provit.service.document;

import com.provit.dto.document.MainResumeJobInfoDTO;

public interface MainResumeService {
    MainResumeJobInfoDTO setMainResume(long userNum, long resumeNum);

    void clearMainResume(long userNum);

    /** 대표이력서가 없으면 null을 반환한다. 직군·직무는 회원이 아닌 이력서에서 조회한다. */
    MainResumeJobInfoDTO getMainResumeJobInfo(long userNum);
}

package com.provit.dao.document;

import com.provit.dto.document.MainResumeJobInfoDTO;

public interface MainResumeDAO {
    int updateMainResume(long userNum, long resumeNum);

    int clearMainResume(long userNum);

    MainResumeJobInfoDTO selectMainResumeJobInfo(long userNum);
}

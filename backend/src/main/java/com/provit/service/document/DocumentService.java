package com.provit.service.document;

import com.provit.dto.user.ResumeDetailDTO;

public interface DocumentService {

    ResumeDetailDTO createResume(int userNum, ResumeDetailDTO resumeDetail);
}

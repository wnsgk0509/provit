package com.provit.service.study;

import com.provit.dto.study.StudyDTO;
import java.util.List;

public interface StudyService {
    List<StudyDTO> getStudyList(Long userNum);
    StudyDTO getStudyDetail(Long studyNum);
    Long createStudy(StudyDTO studyDto);
    void deleteStudy(Long studyNum);
    void joinStudy(Long studyNum, Long userNum);
    void leaveStudy(Long studyNum, Long userNum);
}

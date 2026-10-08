package com.provit.service.study;

import com.provit.dto.study.StudyDTO;
import java.util.List;

public interface StudyService {
    List<StudyDTO> getStudyList(Long userNum);
    StudyDTO getStudyDetail(Long studyNum);
    Long createStudy(StudyDTO studyDto);
    int updateStudy(StudyDTO studyDto);
    void deleteStudy(Long studyNum, Long userNum);
    void joinStudy(Long studyNum, Long userNum);
    void leaveStudy(Long studyNum, Long userNum);
    List<String> getStudyMembers(Long studyNum);
}

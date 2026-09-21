package com.provit.dao.study;

import com.provit.dto.study.StudyDTO;
import java.util.List;
import java.util.Map;

public interface StudyDAO {
    List<StudyDTO> selectStudyList(Long userNum);
    StudyDTO selectStudyDetail(Long studyNum);
    int insertStudy(StudyDTO studyDto);
    int deleteStudy(Long studyNum);
    
    // Member
    int insertStudyMember(Map<String, Object> params);
    int deleteStudyMember(Map<String, Object> params);
    int checkStudyMember(Map<String, Object> params);
}

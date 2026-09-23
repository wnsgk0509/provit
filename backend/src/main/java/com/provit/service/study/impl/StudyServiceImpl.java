package com.provit.service.study.impl;

import com.provit.dao.study.StudyDAO;
import com.provit.dto.study.StudyDTO;
import com.provit.service.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyServiceImpl implements StudyService {

    private final StudyDAO studyDao;

    @Autowired
    public StudyServiceImpl(StudyDAO studyDao) {
        this.studyDao = studyDao;
    }

    @Override
    public List<StudyDTO> getStudyList(Long userNum) {
        return studyDao.selectStudyList(userNum);
    }

    @Override
    public StudyDTO getStudyDetail(Long studyNum) {
        return studyDao.selectStudyDetail(studyNum);
    }

    @Override
    @Transactional
    public Long createStudy(StudyDTO studyDto) {
        // 1. 스터디 방 개설
        studyDao.insertStudy(studyDto);
        Long newStudyNum = studyDto.getStudyNum();
        
        // 2. 개설자를 스터디 멤버로 자동 등록
        Map<String, Object> params = new HashMap<>();
        params.put("studyNum", newStudyNum);
        params.put("userNum", studyDto.getUserNum());
        studyDao.insertStudyMember(params);
        
        return newStudyNum;
    }

    @Override
    @Transactional
    public int updateStudy(StudyDTO studyDto) {
        int affectedRows = studyDao.updateStudy(studyDto);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("스터디 방이 존재하지 않거나 권한이 없습니다.");
        }
        return affectedRows;
    }

    @Override
    @Transactional
    public void deleteStudy(Long studyNum, Long userNum) {
        // DB 테이블에 ON DELETE CASCADE가 걸려 있으므로, 
        // 방을 지우면 참여자 명단도 자동으로 지워짐
        Map<String, Object> params = new HashMap<>();
        params.put("studyNum", studyNum);
        params.put("userNum", userNum);
        int affectedRows = studyDao.deleteStudy(params);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("스터디 방이 존재하지 않거나 권한이 없습니다.");
        }
    }

    @Override
    @Transactional
    public void joinStudy(Long studyNum, Long userNum) {
        Map<String, Object> params = new HashMap<>();
        params.put("studyNum", studyNum);
        params.put("userNum", userNum);
        
        // 중복 참여 방지 로직 (선택사항)
        if (studyDao.checkStudyMember(params) == 0) {
            studyDao.insertStudyMember(params);
        }
    }

    @Override
    @Transactional
    public void leaveStudy(Long studyNum, Long userNum) {
        Map<String, Object> params = new HashMap<>();
        params.put("studyNum", studyNum);
        params.put("userNum", userNum);
        
        studyDao.deleteStudyMember(params);
    }

    @Override
    public List<String> getStudyMembers(Long studyNum) {
        return studyDao.selectStudyMembers(studyNum);
    }
}

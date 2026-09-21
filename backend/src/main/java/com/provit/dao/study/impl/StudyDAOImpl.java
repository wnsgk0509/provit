package com.provit.dao.study.impl;

import com.provit.dao.study.StudyDAO;
import com.provit.dto.study.StudyDTO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class StudyDAOImpl implements StudyDAO {

    private final SqlSession sqlSession;
    private static final String NAMESPACE = "com.provit.dao.study.StudyDAO.";

    @Autowired
    public StudyDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public List<StudyDTO> selectStudyList(Long userNum) {
        return sqlSession.selectList(NAMESPACE + "selectStudyList", userNum);
    }

    @Override
    public StudyDTO selectStudyDetail(Long studyNum) {
        return sqlSession.selectOne(NAMESPACE + "selectStudyDetail", studyNum);
    }

    @Override
    public int insertStudy(StudyDTO studyDto) {
        return sqlSession.insert(NAMESPACE + "insertStudy", studyDto);
    }

    @Override
    public int updateStudy(StudyDTO studyDto) {
        return sqlSession.update(NAMESPACE + "updateStudy", studyDto);
    }

    @Override
    public int deleteStudy(Long studyNum) {
        return sqlSession.delete(NAMESPACE + "deleteStudy", studyNum);
    }

    @Override
    public int insertStudyMember(Map<String, Object> params) {
        return sqlSession.insert(NAMESPACE + "insertStudyMember", params);
    }

    @Override
    public int deleteStudyMember(Map<String, Object> params) {
        return sqlSession.delete(NAMESPACE + "deleteStudyMember", params);
    }

    @Override
    public int checkStudyMember(Map<String, Object> params) {
        return sqlSession.selectOne(NAMESPACE + "checkStudyMember", params);
    }
}

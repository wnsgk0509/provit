package com.provit.dao.recruitment.impl;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;

@Repository
public class RecruitmentDAOImpl implements RecruitmentDAO {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	public RecruitmentDAOImpl() {
	}

	public RecruitmentDAOImpl(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public List<OccupationDTO> selectOccupationList() {
		return sqlSessionTemplate.selectList("recruitment_mapper.selectOccupationList");
	}

	@Override
	public List<JobDTO> selectJobListByOccupation(String occupationCode) {
		return sqlSessionTemplate.selectList("recruitment_mapper.selectJobListByOccupation", occupationCode);
	}

	@Override
	public int mergeRecruitment(RecruitmentDTO recruitmentDTO) {
		return sqlSessionTemplate.insert("recruitment_mapper.mergeRecruitment", recruitmentDTO);
	}

	@Override
	public List<RecruitmentDTO> selectRecruitmentList(RecruitmentSearchDTO searchDTO) {
		return sqlSessionTemplate.selectList("recruitment_mapper.selectRecruitmentList", searchDTO);
	}

	@Override
	public long selectRecruitmentCount(RecruitmentSearchDTO searchDTO) {
		return sqlSessionTemplate.selectOne("recruitment_mapper.selectRecruitmentCount", searchDTO);
	}

	@Override
	public int deactivateExpiredRecruitments() {
		return sqlSessionTemplate.update("recruitment_mapper.deactivateExpiredRecruitments");
	}

	@Override
	public int insertJobScrap(long recruitmentNum, long userNum) {
		java.util.Map<String, Object> params = new java.util.HashMap<>();
		params.put("recruitmentNum", recruitmentNum);
		params.put("userNum", userNum);
		return sqlSessionTemplate.insert("recruitment_mapper.insertJobScrap", params);
	}

	@Override
	public int deleteJobScrap(long recruitmentNum, long userNum) {
		java.util.Map<String, Object> params = new java.util.HashMap<>();
		params.put("recruitmentNum", recruitmentNum);
		params.put("userNum", userNum);
		return sqlSessionTemplate.delete("recruitment_mapper.deleteJobScrap", params);
	}

	@Override
	public int checkJobScrap(long recruitmentNum, long userNum) {
		java.util.Map<String, Object> params = new java.util.HashMap<>();
		params.put("recruitmentNum", recruitmentNum);
		params.put("userNum", userNum);
		Integer count = sqlSessionTemplate.selectOne("recruitment_mapper.checkJobScrap", params);
		return count != null ? count : 0;
	}
}

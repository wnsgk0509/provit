package com.provit.dao.recruitment.impl;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.OccupationDTO;

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
}

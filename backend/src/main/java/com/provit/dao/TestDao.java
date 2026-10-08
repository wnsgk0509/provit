package com.provit.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 팀원 참고용 샘플 DAO (생성자 주입 방식)
 */
@Repository
public class TestDao {

    private final SqlSession sqlSession;

    // 생성자 주입 (Constructor Injection)
    @Autowired
    public TestDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    private static final String NAMESPACE = "com.provit.mapper.SampleMapper";

    /**
     * 오라클 DB 현재 시각 조회 테스트
     */
    public String selectCurrentTime() {
        try {
            return sqlSession.selectOne(NAMESPACE + ".selectCurrentTime");
        } catch (Exception e) {
            return "DB 연결 대기 중 (미연결 시 기본값 반환)";
        }
    }
}

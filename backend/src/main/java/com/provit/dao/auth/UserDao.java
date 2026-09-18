package com.provit.dao.auth;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dto.auth.UserDto;

/**
 * T_USER 테이블 접근 DAO (생성자 주입 방식)
 */
@Repository
public class UserDao {

    private static final String NAMESPACE = "com.provit.mapper.UserMapper";

    private final SqlSession sqlSession;

    @Autowired
    public UserDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    /**
     * 이메일로 회원 단건 조회 (로그인 및 중복 검사용)
     */
    public UserDto selectByEmail(String userEmail) {
        return sqlSession.selectOne(NAMESPACE + ".selectByEmail", userEmail);
    }

    /**
     * 닉네임으로 회원 단건 조회 (중복 검사용)
     */
    public UserDto selectByNickname(String userNickname) {
        return sqlSession.selectOne(NAMESPACE + ".selectByNickname", userNickname);
    }

    /**
     * 회원 번호(USER_NUM)로 회원 단건 조회
     */
    public UserDto selectByUserNum(Long userNum) {
        return sqlSession.selectOne(NAMESPACE + ".selectByUserNum", userNum);
    }

    /**
     * 이메일 중복 건수 조회
     */
    public int countByEmail(String userEmail) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countByEmail", userEmail);
        return count != null ? count : 0;
    }

    /**
     * 닉네임 중복 건수 조회
     */
    public int countByNickname(String userNickname) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countByNickname", userNickname);
        return count != null ? count : 0;
    }

    /**
     * 신규 회원 가입 INSERT (시퀀스 채번)
     */
    public int insertUser(UserDto userDto) {
        return sqlSession.insert(NAMESPACE + ".insertUser", userDto);
    }
}

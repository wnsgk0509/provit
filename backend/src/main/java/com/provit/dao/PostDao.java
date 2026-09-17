package com.provit.dao;

import com.provit.dto.community.PostDto;
import com.provit.dto.community.PostSearchDto;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시판 DB 접근을 담당하는 DAO (Data Access Object)
 */
@Repository
public class PostDao {

    // Spring이 생성해둔 SqlSession(DB 연결부)을 주입받아 사용합니다.
    private final SqlSession sqlSession;
    
    // mapper.xml의 namespace와 일치해야 합니다.
    private static final String NAMESPACE = "com.provit.mapper.PostMapper";

    @Autowired
    public PostDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    /**
     * 조건에 맞는 게시글 목록을 DB에서 조회합니다.
     */
    public List<PostDto> selectPostList(PostSearchDto searchDto) {
        // NAMESPACE 뒤에 mapper.xml의 id를 붙여서 실행합니다.
        return sqlSession.selectList(NAMESPACE + ".selectPostList", searchDto);
    }

    /**
     * 조건에 맞는 전체 게시글 갯수를 조회합니다.
     */
    public int countPosts(PostSearchDto searchDto) {
        return sqlSession.selectOne(NAMESPACE + ".countPosts", searchDto);
    }
}

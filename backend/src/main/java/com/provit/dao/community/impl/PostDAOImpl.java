package com.provit.dao.community.impl;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.community.PostDAO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;

/**
 * 게시판 DB 접근을 담당하는 DAO (Data Access Object)
 */
@Repository
public class PostDAOImpl implements PostDAO {

    // Spring이 생성해둔 SqlSession(DB 연결부)을 주입받아 사용합니다.
    private final SqlSession sqlSession;
    
    // mapper.xml의 namespace와 일치해야 합니다.
    private static final String NAMESPACE = "com.provit.mapper.PostMapper";

    @Autowired
    public PostDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    /**
     * 조건에 맞는 게시글 목록을 DB에서 조회합니다.
     */
    public List<PostDTO> selectPostList(PostSearchDTO searchDto) {
        // NAMESPACE 뒤에 mapper.xml의 id를 붙여서 실행합니다.
        return sqlSession.selectList(NAMESPACE + ".selectPostList", searchDto);
    }

    /**
     * 조건에 맞는 전체 게시글 갯수를 조회합니다.
     */
    public int countPosts(PostSearchDTO searchDto) {
        return sqlSession.selectOne(NAMESPACE + ".countPosts", searchDto);
    }
    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     */
    public PostDTO selectPostDetail(Long postNum) {
        return sqlSession.selectOne(NAMESPACE + ".selectPostDetail", postNum);
    }

    /**
     * 특정 게시글의 조회수를 1 증가시킵니다.
     */
    public int updateViewCount(Long postNum) {
        return sqlSession.update(NAMESPACE + ".updateViewCount", postNum);
    }

    /**
     * 새 게시글을 등록합니다.
     */
    public int insertPost(PostDTO postDto) {
        return sqlSession.insert(NAMESPACE + ".insertPost", postDto);
    }

    /**
     * 게시글을 수정합니다.
     */
    public int updatePost(PostDTO postDto) {
        return sqlSession.update(NAMESPACE + ".updatePost", postDto);
    }

    /**
     * 게시글을 삭제합니다.
     */
    public int deletePost(java.util.Map<String, Object> params) {
        return sqlSession.delete(NAMESPACE + ".deletePost", params);
    }
}

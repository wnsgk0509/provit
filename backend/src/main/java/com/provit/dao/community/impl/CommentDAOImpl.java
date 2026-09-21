package com.provit.dao.community.impl;

import com.provit.dao.community.CommentDAO;
import com.provit.dto.community.CommentDTO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommentDAOImpl implements CommentDAO {

    private final SqlSession sqlSession;
    private static final String NAMESPACE = "com.provit.dao.community.CommentDAO.";

    @Autowired
    public CommentDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public List<CommentDTO> selectCommentList(Long postNum) {
        return sqlSession.selectList(NAMESPACE + "selectCommentList", postNum);
    }

    @Override
    public int insertComment(CommentDTO commentDto) {
        return sqlSession.insert(NAMESPACE + "insertComment", commentDto);
    }

    @Override
    public int updateComment(CommentDTO commentDto) {
        return sqlSession.update(NAMESPACE + "updateComment", commentDto);
    }

    @Override
    public int deleteComment(Long commentNum) {
        return sqlSession.delete(NAMESPACE + "deleteComment", commentNum);
    }
}

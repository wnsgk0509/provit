package com.provit.service.community.impl;

import com.provit.dao.community.CommentDAO;
import com.provit.dto.community.CommentDTO;
import com.provit.service.community.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentDAO commentDao;

    @Autowired
    public CommentServiceImpl(CommentDAO commentDao) {
        this.commentDao = commentDao;
    }

    @Override
    public List<CommentDTO> getCommentList(Long postNum) {
        return commentDao.selectCommentList(postNum);
    }

    @Override
    @Transactional
    public Long createComment(CommentDTO commentDto) {
        if (commentDto.getParentCommentNum() != null) {
            CommentDTO parentComment = commentDao.selectCommentDetail(commentDto.getParentCommentNum());
            if (parentComment == null) {
                throw new IllegalArgumentException("부모 댓글이 존재하지 않습니다.");
            }
            // 인스타그램 방식: 대댓글에 답글을 달아도 원본 부모의 대댓글로 편입 (1단계 계층형 유지)
            if (parentComment.getParentCommentNum() != null) {
                commentDto.setParentCommentNum(parentComment.getParentCommentNum());
            }
        }
        
        commentDao.insertComment(commentDto);
        return commentDto.getCommentNum();
    }

    @Override
    @Transactional
    public void updateComment(CommentDTO commentDto) {
        int affectedRows = commentDao.updateComment(commentDto);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("댓글이 존재하지 않거나 권한이 없습니다.");
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long commentNum, Long userNum) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("commentNum", commentNum);
        params.put("userNum", userNum);
        int affectedRows = commentDao.deleteComment(params);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("댓글이 존재하지 않거나 권한이 없습니다.");
        }
    }
}

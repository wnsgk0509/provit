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
        commentDao.insertComment(commentDto);
        return commentDto.getCommentNum();
    }

    @Override
    @Transactional
    public void updateComment(CommentDTO commentDto) {
        commentDao.updateComment(commentDto);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentNum) {
        commentDao.deleteComment(commentNum);
    }
}

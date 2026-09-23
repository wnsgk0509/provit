package com.provit.dao.community;

import com.provit.dto.community.CommentDTO;
import java.util.List;

public interface CommentDAO {
    List<CommentDTO> selectCommentList(Long postNum);
    CommentDTO selectCommentDetail(Long commentNum);
    int insertComment(CommentDTO commentDto);
    int updateComment(CommentDTO commentDto);
    int deleteComment(java.util.Map<String, Object> params);
}

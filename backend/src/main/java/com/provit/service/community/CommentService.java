package com.provit.service.community;

import com.provit.dto.community.CommentDTO;
import java.util.List;

public interface CommentService {
    List<CommentDTO> getCommentList(Long postNum);
    Long createComment(CommentDTO commentDto);
    void updateComment(CommentDTO commentDto);
    void deleteComment(Long commentNum, Long userNum);
}

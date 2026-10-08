package com.provit.controller.community;

import com.provit.dto.community.CommentDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.community.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.provit.common.annotation.LoginUser;
import com.provit.common.ResponseCode;

import java.util.List;

@RestController
@RequestMapping("/api/community/posts/{postNum}/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    @GetMapping
    public ApiResponse<List<CommentDTO>> getCommentList(@PathVariable Long postNum) {
        List<CommentDTO> list = commentService.getCommentList(postNum);
        return ApiResponse.success(list);
    }

    /**
     * 새 댓글 등록
     */
    @PostMapping
    public ApiResponse<Long> createComment(
            @PathVariable Long postNum, 
            @RequestBody CommentDTO commentDto,
            @LoginUser Long userNum) {
        
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        
        // URL의 postNum을 DTO에 강제 주입하여 무결성 유지
        commentDto.setPostNum(postNum);
        commentDto.setUserNum(userNum);
        Long createdCommentNum = commentService.createComment(commentDto);
        return ApiResponse.success(createdCommentNum);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentNum}")
    public ApiResponse<Void> updateComment(
            @PathVariable Long postNum,
            @PathVariable Long commentNum,
            @RequestBody CommentDTO commentDto,
            @LoginUser Long userNum) {
            
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
            
        commentDto.setPostNum(postNum);
        commentDto.setCommentNum(commentNum);
        commentDto.setUserNum(userNum);
        commentService.updateComment(commentDto);
        return ApiResponse.success();
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentNum}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postNum,
            @PathVariable Long commentNum,
            @LoginUser Long userNum) {
            
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
            
        commentService.deleteComment(commentNum, userNum);
        return ApiResponse.success();
    }
}

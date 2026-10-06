package com.provit.controller.community;

import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.community.CommentDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.community.CommentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/comments")
public class MyCommentController {

    private final CommentService commentService;

    public MyCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/me")
    public ApiResponse<List<CommentDTO>> getMyComments(@LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        return ApiResponse.success(commentService.getMyCommentList(userNum));
    }
}

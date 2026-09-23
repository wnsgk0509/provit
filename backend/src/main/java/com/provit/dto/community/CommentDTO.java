package com.provit.dto.community;

import java.util.Date;

/**
 * T_COMMENT 테이블 매핑 및 프론트엔드 전달용 DTO
 */
public class CommentDTO {
    private Long commentNum;
    private Long postNum;
    private Long userNum;
    private String commentContent;
    private String commentDate; // 포맷팅된 문자열 반환을 위해 String 사용 고려, 혹은 Date
    private String userNickname; // T_USER 조인용

    public Long getCommentNum() {
        return commentNum;
    }

    public void setCommentNum(Long commentNum) {
        this.commentNum = commentNum;
    }

    public Long getPostNum() {
        return postNum;
    }

    public void setPostNum(Long postNum) {
        this.postNum = postNum;
    }

    public Long getUserNum() {
        return userNum;
    }

    public void setUserNum(Long userNum) {
        this.userNum = userNum;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public String getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(String commentDate) {
        this.commentDate = commentDate;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }
}

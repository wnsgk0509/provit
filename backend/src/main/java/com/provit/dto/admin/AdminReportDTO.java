package com.provit.dto.admin;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportDTO {
    private Long reportNum;
    private Long reporterNum;
    private String reporterNickname; // T_USER 조인
    
    private String targetType; // 'POST' or 'COMMENT'
    private Long targetNum;
    private String targetContentPreview; // 게시글 제목 또는 댓글 내용 앞부분
    private Long commentPostNum; // 댓글 신고 시 원본 게시글 번호
    
    private String reportReason;
    private String reportStatus; // PENDING, RESOLVED, REJECTED
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date reportDate;
}

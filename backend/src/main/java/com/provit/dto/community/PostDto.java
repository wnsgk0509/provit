package com.provit.dto.community;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 커뮤니티 게시글 단건 데이터를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PostDto {
    // 1. T_POST 테이블 기본 컬럼
    private Long postNum;          // 게시글 고유 번호
    private Integer categoryNum;   // 카테고리 번호
    private Integer userNum;       // 작성자 회원 번호
    private String postTitle;      // 게시글 제목
    private String postContent;    // 게시글 본문
    private Integer postLikeCount; // 좋아요 수
    private Integer viewCount;     // 조회수
    private String postFile;       // 첨부 파일 URL (선택)

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date postDate;         // 작성 일시

    // 2. JOIN 쿼리를 통해 가져올 추가 정보 (화면 표시용)
    private String categoryName;   // 소속 카테고리 이름 (T_CATEGORY)
    private String userNickname;   // 작성자 닉네임 (T_USER)
}

package com.provit.service.community;

import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;

/**
 * 게시판 비즈니스 로직을 정의하는 서비스 인터페이스 (주방장 역할 설계도)
 */
public interface PostService {
    
    /**
     * 게시글 목록을 페이징 처리하여 반환합니다.
     */
    PageResponseDTO<PostDTO> getPostList(PostSearchDTO searchDto);
    
    /**
     * 특정 게시글의 상세 정보를 조회합니다. (조건에 따라 조회수 증가)
     */
    PostDTO getPostDetail(Long postNum, Long userNum, boolean shouldIncreaseViewCount);

    /**
     * 특정 게시글의 좋아요 상태를 토글(On/Off)합니다.
     * @return Map containing 'isLiked' (boolean) and 'likeCount' (int)
     */
    java.util.Map<String, Object> togglePostLike(Long postNum, Long userNum);

    /**
     * 새 게시글을 등록합니다.
     */
    Long createPost(PostDTO postDto);

    /**
     * 기존 게시글을 수정합니다.
     */
    void updatePost(PostDTO postDto);

    /**
     * 특정 게시글을 삭제합니다.
     */
    void deletePost(Long postNum, Long userNum);

    /**
     * 인기 게시글을 조회합니다.
     */
    java.util.List<PostDTO> getPopularPosts(int limit);
}

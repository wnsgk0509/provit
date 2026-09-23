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
     * 특정 게시글의 상세 정보를 조회합니다. (조회수 증가 포함)
     */
    PostDTO getPostDetail(Long postNum);

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
}

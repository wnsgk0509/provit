package com.provit.service;

import com.provit.dto.common.PageResponseDto;
import com.provit.dto.community.PostDto;
import com.provit.dto.community.PostSearchDto;

/**
 * 게시판 비즈니스 로직을 정의하는 서비스 인터페이스 (주방장 역할 설계도)
 */
public interface PostService {
    
    /**
     * 게시글 목록을 페이징 처리하여 반환합니다.
     */
    PageResponseDto<PostDto> getPostList(PostSearchDto searchDto);
    
}

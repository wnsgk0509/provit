package com.provit.service.impl;

import com.provit.dao.PostDao;
import com.provit.dto.common.PageResponseDto;
import com.provit.dto.community.PostDto;
import com.provit.dto.community.PostSearchDto;
import com.provit.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시판 비즈니스 로직을 실제 수행하는 서비스 구현체 (주방장)
 */
@Service
public class PostServiceImpl implements PostService {

    private final PostDao postDao;

    // DAO(창고 관리인)를 주입받습니다.
    @Autowired
    public PostServiceImpl(PostDao postDao) {
        this.postDao = postDao;
    }

    @Override
    public PageResponseDto<PostDto> getPostList(PostSearchDto searchDto) {
        // 1. 전체 게시글 갯수 조회 (페이징의 '총 페이지 수'를 알기 위해 필수)
        int totalCount = postDao.countPosts(searchDto);

        // 2. 조건에 맞는 게시글 목록 조회 (OFFSET과 PAGE_SIZE 기반으로 잘라서 가져옴)
        List<PostDto> list = postDao.selectPostList(searchDto);

        // 3. 우리가 만들어둔 공통 페이징 응답 객체(접시)에 데이터와 페이징 정보를 예쁘게 담아 반환
        return new PageResponseDto<>(list, totalCount, searchDto.getPage(), searchDto.getPageSize());
    }
}

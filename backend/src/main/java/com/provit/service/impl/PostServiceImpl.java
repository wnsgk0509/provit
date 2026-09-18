package com.provit.service.impl;

import com.provit.dao.PostDAO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;
import com.provit.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시판 비즈니스 로직을 실제 수행하는 서비스 구현체 (주방장)
 */
@Service
public class PostServiceImpl implements PostService {

    private final PostDAO postDao;

    // DAO(창고 관리인)를 주입받습니다.
    @Autowired
    public PostServiceImpl(PostDAO postDao) {
        this.postDao = postDao;
    }

    @Override
    public PageResponseDTO<PostDTO> getPostList(PostSearchDTO searchDto) {
        // 1. 전체 게시글 수 조회 (페이징 계산을 위해 필요)
        int totalCount = postDao.countPosts(searchDto);

        // 2. 현재 페이지에 노출될 게시글 목록 조회
        List<PostDTO> list = postDao.selectPostList(searchDto);

        // 3. 조회된 목록과 전체 수, 페이지 정보를 담은 통합 응답 객체를 반환
        return new PageResponseDTO<>(list, totalCount, searchDto.getPage(), searchDto.getPageSize());
    }
}

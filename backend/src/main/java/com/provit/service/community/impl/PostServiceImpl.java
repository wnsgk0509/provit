package com.provit.service.community.impl;

import com.provit.dao.community.PostDAO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;
import com.provit.service.community.PostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PostDTO getPostDetail(Long postNum) {
        // 1. 상세 조회 시 조회수를 1 증가시킵니다.
        postDao.updateViewCount(postNum);
        
        // 2. 최신 정보(증가된 조회수 포함)로 게시글 데이터를 조회하여 반환합니다.
        return postDao.selectPostDetail(postNum);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Long createPost(PostDTO postDto) {
        // DB에 삽입 (MyBatis selectKey 기능으로 postDto에 생성된 PK가 담김)
        postDao.insertPost(postDto);
        return postDto.getPostNum();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updatePost(PostDTO postDto) {
        int affectedRows = postDao.updatePost(postDto);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("게시글이 존재하지 않거나 권한이 없습니다.");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deletePost(Long postNum, Long userNum) {
        Map<String, Object> params = new HashMap<>();
        params.put("postNum", postNum);
        params.put("userNum", userNum);
        int affectedRows = postDao.deletePost(params);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("게시글이 존재하지 않거나 권한이 없습니다.");
        }
    }
}

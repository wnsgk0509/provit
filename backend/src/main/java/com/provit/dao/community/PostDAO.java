package com.provit.dao.community;

public interface PostDAO {
    java.util.List<com.provit.dto.community.PostDTO> selectPostList(com.provit.dto.community.PostSearchDTO searchDto);
    int countPosts(com.provit.dto.community.PostSearchDTO searchDto);
    com.provit.dto.community.PostDTO selectPostDetail(Long postNum);
    int updateViewCount(Long postNum);
    int insertPost(com.provit.dto.community.PostDTO postDto);
    int updatePost(com.provit.dto.community.PostDTO postDto);
    int deletePost(java.util.Map<String, Object> params);
}

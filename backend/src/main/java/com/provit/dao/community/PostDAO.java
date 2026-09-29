package com.provit.dao.community;

public interface PostDAO {
    java.util.List<com.provit.dto.community.PostDTO> selectPostList(com.provit.dto.community.PostSearchDTO searchDto);
    int countPosts(com.provit.dto.community.PostSearchDTO searchDto);
    com.provit.dto.community.PostDTO selectPostDetail(Long postNum);
    int updateViewCount(Long postNum);
    int insertPost(com.provit.dto.community.PostDTO postDto);
    int updatePost(com.provit.dto.community.PostDTO postDto);
    int deletePost(java.util.Map<String, Object> params);

    // --- 좋아요(공감) 관련 ---
    
    /**
     * 특정 회원이 해당 게시글에 좋아요를 눌렀는지 확인합니다.
     */
    int checkPostLike(java.util.Map<String, Object> params);

    /**
     * 게시글에 좋아요를 추가합니다.
     */
    int insertPostLike(java.util.Map<String, Object> params);

    /**
     * 게시글의 좋아요를 취소(삭제)합니다.
     */
    int deletePostLike(java.util.Map<String, Object> params);

    /**
     * 게시글의 총 좋아요 수를 증감시킵니다. (amount: +1 또는 -1)
     */
    int updatePostLikeCount(java.util.Map<String, Object> params);
}


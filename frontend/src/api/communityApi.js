import client from './client';

/**
 * 커뮤니티 게시글 목록 조회
 * @param {Object} params - 검색 및 페이징 파라미터 (page, categoryNum, searchType, keyword)
 * @returns {Promise<Object>} ApiResponse<PageResponseDto<PostDto>> 형태의 데이터 반환
 */
export const fetchPostList = async (params) => {
    try {
        const response = await client.get('/community/posts', { params });
        return response.data;
    } catch (error) {
        console.error('커뮤니티 게시글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 현재 로그인한 사용자가 작성한 게시글 목록 조회
 * @param {Object} params - 페이지 파라미터 (page, pageSize)
 */
export const fetchMyPosts = async (params) => {
    try {
        const response = await client.get('/community/posts/me', { params });
        return response.data;
    } catch (error) {
        console.error('내 게시글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 인기 게시글 목록 조회
 * @param {number} limit - 가져올 게시글 수
 */
export const fetchPopularPosts = async (limit = 4) => {
    try {
        const response = await client.get(`/community/posts/popular?limit=${limit}`);
        return response.data;
    } catch (error) {
        console.error('인기 게시글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 게시글 상세 조회
 * @param {number|string} postNum - 게시글 번호
 */
export const fetchPostDetail = async (postNum) => {
    try {
        const response = await client.get(`/community/posts/${postNum}`);
        return response.data;
    } catch (error) {
        console.error('게시글 상세 조회 실패:', error);
        throw error;
    }
};

/**
 * 게시글 작성
 * @param {Object} postDto - 게시글 데이터
 */
export const createPost = async (postDto) => {
    try {
        const response = await client.post('/community/posts', postDto);
        return response.data;
    } catch (error) {
        console.error('게시글 작성 실패:', error);
        throw error;
    }
};

/**
 * 게시글 수정
 * @param {Object} postDto - 수정할 게시글 데이터
 */
export const updatePost = async (postDto) => {
    try {
        const response = await client.put(`/community/posts/${postDto.postNum}`, postDto);
        return response.data;
    } catch (error) {
        console.error('게시글 수정 실패:', error);
        throw error;
    }
};

/**
 * 게시글 삭제
 * @param {number|string} postNum - 삭제할 게시글 번호
 */
export const deletePost = async (postNum) => {
    try {
        const response = await client.delete(`/community/posts/${postNum}`);
        return response.data;
    } catch (error) {
        console.error('게시글 삭제 실패:', error);
        throw error;
    }
};

/**
 * 게시글 좋아요 토글
 * @param {number|string} postNum - 게시글 번호
 */
export const togglePostLike = async (postNum) => {
    try {
        const response = await client.post(`/community/posts/${postNum}/like`);
        return response.data;
    } catch (error) {
        console.error('게시글 좋아요 토글 실패:', error);
        throw error;
    }
};

/**
 * 게시글/댓글 신고 접수
 * @param {Object} reportDto - 신고 데이터 { targetType, targetNum, reportReason }
 */
export const submitReport = async (reportDto) => {
    try {
        const response = await client.post('/community/report', reportDto);
        return response.data;
    } catch (error) {
        console.error('신고 접수 실패:', error);
        throw error;
    }
};

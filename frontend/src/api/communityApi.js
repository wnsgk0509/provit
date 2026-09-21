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
        const response = await client.put('/community/posts', postDto);
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

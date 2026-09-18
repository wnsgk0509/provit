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

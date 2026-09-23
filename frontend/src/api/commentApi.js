import client from './client';

/**
 * 특정 게시글의 댓글 목록 조회
 */
export const fetchComments = async (postNum) => {
    try {
        const response = await client.get(`/community/posts/${postNum}/comments`);
        return response.data;
    } catch (error) {
        console.error('댓글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 댓글 작성
 */
export const createComment = async (postNum, commentDto) => {
    try {
        const response = await client.post(`/community/posts/${postNum}/comments`, commentDto);
        return response.data;
    } catch (error) {
        console.error('댓글 작성 실패:', error);
        throw error;
    }
};

/**
 * 댓글 수정
 */
export const updateComment = async (postNum, commentNum, commentDto) => {
    try {
        const response = await client.put(`/community/posts/${postNum}/comments/${commentNum}`, commentDto);
        return response.data;
    } catch (error) {
        console.error('댓글 수정 실패:', error);
        throw error;
    }
};

/**
 * 댓글 삭제
 */
export const deleteComment = async (postNum, commentNum) => {
    try {
        const response = await client.delete(`/community/posts/${postNum}/comments/${commentNum}`);
        return response.data;
    } catch (error) {
        console.error('댓글 삭제 실패:', error);
        throw error;
    }
};

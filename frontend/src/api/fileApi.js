import client from './client';

/**
 * 공통 파일 업로드 API 모듈
 * 
 * @param {File} file - 업로드할 File 객체 (input type="file"에서 가져온 파일)
 * @param {'post' | 'profile'} category - 업로드 목적 카테고리
 * @param {number|string} [targetId] - 게시글 번호. 프로필의 경우 로그인 토큰의 userNum 사용.
 * @returns {Promise<{ targetId: number, originalFileName: string, savedFileName: string, fileUrl: string, fileSize: number, category: string, uploadedAt: string }>}
 */
export const uploadFile = async (file, category, targetId = null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (targetId !== null && targetId !== undefined) {
        formData.append('targetId', targetId);
    }

    const response = await client.post(`/upload/${category}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });

    return response.data.data;
};

/**
 * 저장된 파일 삭제 API 모듈
 * 
 * @param {'post' | 'profile'} category - 소속 카테고리
 * @param {string} savedFileName - 삭제할 저장 파일명
 */
export const deleteFile = async (category, savedFileName) => {
    const response = await client.delete(`/upload/${category}/${savedFileName}`);
    return response.data;
};

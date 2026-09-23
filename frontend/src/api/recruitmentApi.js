import client from './client';

/**
 * 채용 공고 페이징 & 필터링 목록 조회
 * @param {Object} params - { page, size, keyword, location, experienceLevel }
 * @returns {Promise<Object>} ApiResponse<PageResponse<RecruitmentDTO>>
 */
export const fetchRecruitments = async (params) => {
    try {
        const response = await client.get('/recruitment', { params });
        return response.data;
    } catch (error) {
        console.error('채용 공고 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 대분류 직군 목록 조회 (21개 직군)
 * @returns {Promise<Object>} ApiResponse<List<OccupationDTO>>
 */
export const fetchOccupations = async () => {
    try {
        const response = await client.get('/occupation');
        return response.data;
    } catch (error) {
        console.error('직군 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 특정 직군에 속한 세부 직무 목록 조회
 * @param {string} occupationCode - 직군 코드 (예: '2' - IT개발·데이터)
 * @returns {Promise<Object>} ApiResponse<List<JobDTO>>
 */
export const fetchJobsByOccupation = async (occupationCode) => {
    try {
        const response = await client.get(`/occupation/${occupationCode}/jobs`);
        return response.data;
    } catch (error) {
        console.error(`직무 목록 조회 실패 (직군: ${occupationCode}):`, error);
        throw error;
    }
};

/**
 * 사람인 실시간 인기 공고 수동 크롤링 동기화 트리거
 * @param {number} limit - 수집할 공고 수 (기본 1000)
 * @returns {Promise<Object>} ApiResponse<String>
 */
export const syncRecruitments = async (limit = 100) => {
    try {
        const response = await client.post('/recruitment/sync', null, {
            params: { limit }
        });
        return response.data;
    } catch (error) {
        console.error('채용 공고 수동 동기화 실패:', error);
        throw error;
    }
};

/**
 * 특정 채용 공고 관심 등록(스크랩/북마크) 토글
 * @param {number|string} recruitmentNum - 채용 공고 고유 식별 번호
 * @returns {Promise<Object>} ApiResponse<JobScrapResponseDTO>
 */
export const toggleJobScrap = async (recruitmentNum) => {
    try {
        const response = await client.post(`/recruitment/${recruitmentNum}/scrap`);
        return response.data;
    } catch (error) {
        console.error(`채용 공고 스크랩 토글 실패 (공고번호: ${recruitmentNum}):`, error);
        throw error;
    }
};


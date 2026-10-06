import client from './client';

/**
 * 오늘의 취업 운세 및 맞춤 추천 공고 조회
 * @returns {Promise<Object>} ApiResponse<TodayFortuneDTO>
 */
export const fetchTodayFortune = async () => {
    try {
        const response = await client.get('/fortune/today');
        return response.data;
    } catch (error) {
        console.error('오늘의 취업 운세 조회 실패:', error);
        throw error;
    }
};

import client from './client';

export const fetchStudyList = async (userNum) => {
    try {
        const url = userNum ? `/studies?userNum=${userNum}` : `/studies`;
        const response = await client.get(url);
        return response.data;
    } catch (error) {
        console.error('스터디 목록 조회 실패:', error);
        throw error;
    }
};

export const createStudy = async (studyDto) => {
    try {
        const response = await client.post(`/studies`, studyDto);
        return response.data;
    } catch (error) {
        console.error('스터디 개설 실패:', error);
        throw error;
    }
};

export const deleteStudy = async (studyNum) => {
    try {
        const response = await client.delete(`/studies/${studyNum}`);
        return response.data;
    } catch (error) {
        console.error('스터디 삭제 실패:', error);
        throw error;
    }
};

export const joinStudy = async (studyNum, userNum) => {
    try {
        const response = await client.post(`/studies/${studyNum}/join`, { userNum });
        return response.data;
    } catch (error) {
        console.error('스터디 참여 실패:', error);
        throw error;
    }
};

export const leaveStudy = async (studyNum, userNum) => {
    try {
        const response = await client.delete(`/studies/${studyNum}/leave?userNum=${userNum}`);
        return response.data;
    } catch (error) {
        console.error('스터디 탈퇴 실패:', error);
        throw error;
    }
};

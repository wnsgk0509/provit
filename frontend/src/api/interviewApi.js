import client from './client.js';

const INTERVIEW_TIMEOUT = 150000;

export async function getInterviewAvailability() {
    const response = await client.get('/interview/availability');
    return response.data.data;
}

export async function getInterviewSession(historyNum) {
    const response = await client.get(`/interview/${historyNum}/session`, { timeout: INTERVIEW_TIMEOUT });
    return response.data.data;
}

export async function discardInterviewSession(historyNum) {
    await client.delete(`/interview/${historyNum}/session`, { timeout: INTERVIEW_TIMEOUT });
}

export async function getInterviewResults() {
    const response = await client.get('/interview/results');
    return response.data.data;
}

export async function getInterviewRecord(historyNum) {
    const response = await client.get(`/interview/${historyNum}/record`);
    return response.data.data;
}

export async function getInterviewDocuments() {
    const response = await client.get('/interview/documents');
    return response.data.data;
}

export async function createInterview(settings, requestId, recruitment = null) {
    const response = await client.post('/interview/start', {
        resumeNum: Number(settings.resumeNum),
        portfolioNum: Number(settings.portfolioNum || 0),
        letterNum: Number(settings.letterNum || 0),
        interviewDifficulty: settings.difficulty,
        requestId,
        recruitment,
    }, { timeout: INTERVIEW_TIMEOUT });
    return response.data.data;
}

export async function submitInterviewAnswer(historyNum, answerRequest) {
    const response = await client.post(`/interview/${historyNum}/answers`, answerRequest, {
        timeout: INTERVIEW_TIMEOUT,
    });
    return response.data.data;
}

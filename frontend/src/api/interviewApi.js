import client from './client';

const INTERVIEW_TIMEOUT = 150000;

export async function getInterviewDocuments() {
    const response = await client.get('/interview/documents');
    return response.data.data;
}

export async function createInterview(settings, requestId) {
    const response = await client.post('/interview/start', {
        resumeNum: Number(settings.resumeNum),
        portfolioNum: Number(settings.portfolioNum || 0),
        letterNum: Number(settings.letterNum || 0),
        interviewStyle: settings.interviewStyle,
        interviewDifficulty: settings.difficulty,
        requestId,
    }, { timeout: INTERVIEW_TIMEOUT });
    return response.data.data;
}

export async function submitInterviewAnswer(historyNum, answerRequest) {
    const response = await client.post(`/interview/${historyNum}/answers`, answerRequest, {
        timeout: INTERVIEW_TIMEOUT,
    });
    return response.data.data;
}

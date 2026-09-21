import client from './client';

export async function getInterviewDocuments() {
    const response = await client.get('/interview/documents');
    return response.data.data;
}

export async function createInterview(settings) {
    const response = await client.post('/interview/start', {
        resumeNum: Number(settings.resumeNum),
        usePortfolio: settings.usePortfolio,
        useCoverLetter: settings.useCoverLetter,
        interviewStyle: settings.interviewStyle,
        interviewDifficulty: settings.difficulty,
    });
    return response.data.data;
}

export async function submitInterviewAnswer(historyNum, answerRequest) {
    const response = await client.post(`/interview/${historyNum}/answers`, answerRequest);
    return response.data.data;
}

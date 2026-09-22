import client from './client';

export async function createResume(resumeDetail) {
    const response = await client.post('/documents/resumes', resumeDetail);
    return response.data.data;
}

import client from './client';

export async function createResume(resumeDetail) {
    const response = await client.post('/documents/resumes', resumeDetail);
    return response.data.data;
}

export async function createPortfolio(portfolioTitle, file) {
    const formData = new FormData();
    formData.append('portfolioTitle', portfolioTitle.trim());
    formData.append('file', file);

    const response = await client.post('/documents/portfolios', formData, {
        timeout: 60000,
    });
    return response.data.data;
}

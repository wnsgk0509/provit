import client from './client';

export async function createResume(resumeDetail) {
    const response = await client.post('/documents/resumes', resumeDetail);
    return response.data.data;
}

export async function createCoverLetter(coverLetter) {
    const response = await client.post('/documents/cover-letters', coverLetter);
    return response.data.data;
}

export async function updateResume(resumeNum, resumeDetail) {
    const response = await client.put(`/documents/resumes/${resumeNum}`, resumeDetail);
    return response.data.data;
}

export async function updateCoverLetter(letterNum, coverLetter) {
    const response = await client.put(`/documents/cover-letters/${letterNum}`, coverLetter);
    return response.data.data;
}

export async function deleteResume(resumeNum) {
    await client.delete(`/documents/resumes/${resumeNum}`);
}

export async function deleteCoverLetter(letterNum) {
    await client.delete(`/documents/cover-letters/${letterNum}`);
}

export async function deletePortfolio(portfolioNum) {
    await client.delete(`/documents/portfolios/${portfolioNum}`);
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

export async function getResume(resumeNum) {
    const response = await client.get(`/documents/resumes/${resumeNum}`);
    return response.data.data;
}

export async function getResumeList() {
    const response = await client.get('/documents/resumes');
    return response.data.data;
}

export async function getCoverLetter(letterNum) {
    const response = await client.get(`/documents/cover-letters/${letterNum}`);
    return response.data.data;
}

export async function getCoverLetterList() {
    const response = await client.get('/documents/cover-letters');
    return response.data.data;
}

export async function getPortfolio(portfolioNum) {
    const response = await client.get(`/documents/portfolios/${portfolioNum}`);
    return response.data.data;
}

export async function getPortfolioList() {
    const response = await client.get('/documents/portfolios');
    return response.data.data;
}

export async function downloadPortfolioFile(portfolioNum) {
    const response = await client.get(`/documents/portfolios/${portfolioNum}/file`, {
        responseType: 'blob',
        timeout: 60000,
    });
    return response.data;
}

import {
    getResumeList,
    getCoverLetterList,
    getPortfolioList,
    getResume,
    getCoverLetter,
    getPortfolio,
} from './documentApi';

import client from './client';

const documentLoaders = {
    resume: { list: getResumeList, detail: getResume },
    'cover-letter': { list: getCoverLetterList, detail: getCoverLetter },
    portfolio: { list: getPortfolioList, detail: getPortfolio },
};

function loadersFor(documentType) {
    const loaders = documentLoaders[documentType];
    if (!loaders) throw new Error('지원하지 않는 문서 종류입니다.');
    return loaders;
}

export async function getReviewDocuments(documentType) {
    const documents = await loadersFor(documentType).list();
    if (!Array.isArray(documents)) throw new Error('문서 목록을 불러오지 못했습니다.');
    return documents;
}

export function getReviewDocument(documentType, documentNum) {
    return loadersFor(documentType).detail(documentNum);
}

export async function requestDocumentReview(request) {
    const response = await client.post('/document-reviews', request, { timeout: 180000 });
    return response.data.data;
}

export async function getDocumentReview(reviewNum) {
    const response = await client.get(`/document-reviews/${encodeURIComponent(reviewNum)}`);
    return response.data.data;
}

export async function getDocumentReviewHistory(offset = 0, pageSize = 20) {
    const response = await client.get('/document-reviews', { params: { offset, pageSize } });
    return response.data.data;
}

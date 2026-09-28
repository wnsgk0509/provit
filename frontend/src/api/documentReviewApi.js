import {
    getResumeList,
    getCoverLetterList,
    getPortfolioList,
    getResume,
    getCoverLetter,
    getPortfolio,
} from './documentApi';

// Enable only after the separate document-review backend is implemented.
export const DOCUMENT_REVIEW_AVAILABLE = false;

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

// One request reviews the required resume and cover letter plus an optional portfolio.
// Future service boundary: { resumeNum, letterNum, portfolioNum, focusAreas, instructions }.
// Never substitute interview evaluation or a sample for a real review response.
export async function requestDocumentReview() {
    throw new Error('AI원클릭첨삭 서비스는 준비 중입니다.');
}

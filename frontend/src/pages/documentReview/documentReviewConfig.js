export const DOCUMENT_REVIEW_TYPES = [
    {
        value: 'resume',
        selectionKey: 'resumeNum',
        required: true,
        label: '이력서',
        description: '경력과 성과를 더 명확하게',
        hint: '직무, 학력, 경력, 자격증과 지원 동기를 확인합니다.',
    },
    {
        value: 'cover-letter',
        selectionKey: 'letterNum',
        required: true,
        label: '자기소개서',
        description: '나의 경험을 설득력 있게',
        hint: '작성한 항목별로 경험의 흐름과 문장 표현을 확인합니다.',
    },
    {
        value: 'portfolio',
        selectionKey: 'portfolioNum',
        required: false,
        label: '포트폴리오',
        description: '프로젝트의 강점을 구체적으로',
        hint: '등록한 PDF를 바탕으로 프로젝트 설명과 성과를 확인합니다.',
    },
];

export const REVIEW_FOCUS_OPTIONS = [
    { value: 'expression', label: '문장 표현 · 가독성', description: '자연스럽고 간결하게 읽히는 표현' },
    { value: 'structure', label: '구성 · 논리', description: '핵심 메시지와 경험의 흐름' },
    { value: 'jobFit', label: '직무 적합성', description: '지원 직무와 경험의 연결' },
    { value: 'evidence', label: '경험 · 성과 구체성', description: '나의 역할과 성과를 뒷받침하는 근거' },
];

export function reviewErrorMessage(error, fallback) {
    const response = error.response?.data;
    if (typeof response?.data === 'string') return response.data;
    return response?.responseCode?.message || error.message || fallback;
}

const date = (value) => value?.slice(0, 10) || '미입력';
const period = (start, end) => `${date(start)} ~ ${date(end)}`;
const text = (value) => value || '작성된 내용이 없습니다.';

// Preserve line breaks and dates for the on-screen source preview.
export function documentPreviewSections(documentType, document) {
    if (documentType === 'cover-letter') {
        return [
            { title: '성장 과정', content: text(document.growthProcess) },
            { title: '성격의 장단점', content: text(document.personalityStrengthsWeaknesses) },
            { title: '문제 해결 경험', content: text(document.problemSolvingExperience) },
            { title: '입사 후 포부', content: text(document.postJoiningAspiration) },
        ];
    }
    if (documentType !== 'resume') return [];
    const resume = document.resume || {};
    return [
        {
            title: '기본 정보',
            content: [
                `지원 직군: ${resume.occupationName || '미입력'}`,
                `지원 직무: ${resume.jobName || '미입력'}`,
                `최종 학력: ${resume.highestLevel || '미입력'}`,
                `학력 구분: ${resume.educationName || '미입력'}`,
                `희망 근무 지역: ${resume.desiredLocation || '미입력'}`,
                `희망 근무 형태: ${resume.desiredWorkType || '미입력'}`,
            ].join('\n'),
        },
        { title: '지원 동기', content: text(resume.motivation) },
        ...(document.educationList || []).map((item, index) => ({
            title: `학력 ${index + 1}`,
            content: `${item.schoolName || '미입력'} · ${item.major || '전공 미입력'}\n${period(item.admissionDate, item.graduationDate)}\n${item.educationStatus || ''}`,
        })),
        ...(document.careerList || []).map((item, index) => ({
            title: `경력 ${index + 1}`,
            content: `${item.companyName || '미입력'}\n${period(item.joinDate, item.resignDate)}\n${text(item.mainDuty)}`,
        })),
        ...(document.certificationList || []).map((item, index) => ({
            title: `자격증 ${index + 1}`,
            content: `${item.certName || '미입력'} · ${item.certGrade || '등급 미입력'}\n취득일: ${date(item.issueDate)}`,
        })),
    ];
}

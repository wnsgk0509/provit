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

export const REVIEW_CUSTOM_MAX_LENGTH = 200;
export const REVIEW_INSTRUCTIONS_MAX_LENGTH = 200;

export const REVIEW_MODE_OPTIONS = [
    {
        value: 'comprehensive',
        label: '전체 종합 첨삭',
        description: '문장 표현, 논리 흐름, 서류 간 일관성, 직무 적합성 등을 전반적으로 확인합니다.',
    },
    {
        value: 'expression',
        label: '문장 표현·가독성 중심',
        description: '어색한 문장, 중복 표현, 장황한 문장, 맞춤법·문법, 읽기 쉬운 표현 중심으로 첨삭합니다.',
    },
    {
        value: 'consistency',
        label: '서류 간 일관성 중심',
        description: '이력서·자기소개서·포트폴리오 사이의 경력 기간, 역할, 기술, 프로젝트 내용 등의 불일치나 모순을 확인합니다.',
    },
    {
        value: 'jobFit',
        label: '직무 적합성 중심',
        description: '지원 직무와 관련된 경험·기술·역량이 충분히 강조됐는지 확인하고 불필요한 내용은 줄이도록 제안합니다.',
    },
    {
        value: 'evidence',
        label: '성과·구체성 중심',
        description: '추상적인 표현이나 단순 업무 나열을 찾아 역할, 행동, 문제 해결 과정, 성과가 구체적으로 드러나도록 첨삭합니다.',
    },
    {
        value: 'custom',
        label: '직접 입력',
        description: '첨삭에서 중점적으로 확인할 기준을 200자 이내로 직접 입력해 주세요.',
    },
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

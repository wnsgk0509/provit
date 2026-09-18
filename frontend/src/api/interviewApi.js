import client from './client';

const TEMP_USER_NUM = 1;

const mockSessions = new Map();

const waitForMockResponse = () => new Promise((resolve) => {
    window.setTimeout(resolve, 350);
});

const summarizeAnswer = (answer) => (
    answer.length > 35 ? `${answer.slice(0, 35)}...` : answer
);

const createDocumentQuestions = (llmRequest) => {
    const context = llmRequest.context;
    const jobName = context.jobPreference?.jobName || '지원 직무';

    return [
        {
            questionId: 'document-1',
            questionType: 'DOCUMENT',
            questionText: `선택한 이력서를 바탕으로 ${jobName}에 지원한 동기와 본인의 강점을 설명해 주세요.`,
        },
        {
            questionId: 'document-2',
            questionType: 'DOCUMENT',
            questionText: context.portfolio
                ? '포트폴리오에서 가장 주도적으로 참여한 프로젝트와 본인이 담당한 역할을 설명해 주세요.'
                : '이력서에 작성한 경험 중 가장 주도적으로 수행한 업무와 본인의 역할을 설명해 주세요.',
        },
        {
            questionId: 'document-3',
            questionType: 'DOCUMENT',
            questionText: context.coverLetter
                ? '자기소개서에 작성한 문제 해결 경험에서 가장 중요하게 판단한 기준은 무엇인가요?'
                : `${jobName} 업무에서 본인의 경험과 기술이 어떤 강점으로 작용할 수 있는지 구체적인 사례와 함께 설명해 주세요.`,
        },
    ];
};

const createFollowUpQuestion = (answerCount, latestAnswer) => {
    if (answerCount === 3) {
        return {
            questionId: 'follow-up-1',
            questionType: 'FOLLOW_UP',
            questionText: `방금 답변에서 “${summarizeAnswer(latestAnswer)}”라고 설명했습니다. 해당 경험에서 예상과 다르게 진행된 부분과 대처 방법을 말씀해 주세요.`,
        };
    }

    return {
        questionId: 'follow-up-2',
        questionType: 'FOLLOW_UP',
        questionText: '앞선 답변의 경험을 다시 수행한다면 어떤 부분을 가장 먼저 개선하겠으며, 그 이유는 무엇인가요?',
    };
};

const createResult = (session) => ({
    interviewId: session.interviewId,
    totalScore: 82,
    interviewStyle: session.settings.interviewStyle,
    difficulty: session.settings.interviewDifficulty,
    scores: [
        { category: 'CONFIDENCE', label: '자신감', score: 84 },
        { category: 'LOGIC', label: '논리성', score: 79 },
        { category: 'EXPERTISE', label: '전문성', score: 86 },
        { category: 'DELIVERY', label: '전달력', score: 80 },
        { category: 'JOB_FIT', label: '직무적합성', score: 83 },
    ],
    feedback: {
        strengths: '프로젝트 경험을 구체적인 상황과 본인의 역할 중심으로 설명해 답변의 신뢰도가 높았습니다.',
        weaknesses: '일부 답변에서 결론이 뒤에 제시되어 핵심 내용을 파악하는 데 시간이 걸렸습니다.',
        comparison: '이전 모의면접 더미 기록보다 총점이 4점 상승했으며, 전문성 항목의 개선 폭이 가장 큽니다.',
        improvements: '답변을 결론, 근거, 실제 사례 순서로 구성하고 각 답변을 1분 30초 안에 마무리해 보세요.',
    },
    completedAt: new Date().toISOString(),
});

const extractResponseData = (response) => response.data.data;

export async function fetchInterviewDocuments() {
    const response = await client.get('/interviews/documents', {
        params: { userNum: TEMP_USER_NUM },
    });

    return extractResponseData(response);
}

export async function fetchResumeDetail(resumeNum) {
    const response = await client.get(`/interviews/resumes/${resumeNum}`, {
        params: { userNum: TEMP_USER_NUM },
    });

    return extractResponseData(response);
}

export async function prepareInterviewContext(settings) {
    const response = await client.post('/interviews/context', {
        resumeNum: Number(settings.resumeNum),
        usePortfolio: settings.usePortfolio,
        useCoverLetter: settings.useCoverLetter,
        interviewStyle: settings.interviewStyle,
        interviewDifficulty: settings.interviewDifficulty,
    }, {
        params: { userNum: TEMP_USER_NUM },
    });

    return extractResponseData(response);
}

export async function createInterview(settings) {
    const llmRequest = await prepareInterviewContext(settings);
    await waitForMockResponse();

    const interviewId = `mock-interview-${Date.now()}`;
    const session = {
        interviewId,
        settings: { ...settings },
        llmRequest,
        questions: createDocumentQuestions(llmRequest),
        answers: [],
    };

    mockSessions.set(interviewId, session);

    return {
        interviewId,
        questions: session.questions,
        answerTimeLimitSeconds: 120,
    };
}

export async function submitInterviewAnswer(interviewId, answerRequest) {
    await waitForMockResponse();

    const session = mockSessions.get(interviewId);
    if (!session) {
        throw new Error('INTERVIEW_NOT_FOUND');
    }

    session.answers.push({ ...answerRequest });
    const answerCount = session.answers.length;

    if (answerCount >= 5) {
        return {
            completed: true,
            result: createResult(session),
        };
    }

    if (answerCount >= 3) {
        const nextQuestion = createFollowUpQuestion(answerCount, answerRequest.answer);
        session.questions.push(nextQuestion);

        return {
            completed: false,
            nextQuestion,
        };
    }

    return {
        completed: false,
        nextQuestion: session.questions[answerCount],
    };
}

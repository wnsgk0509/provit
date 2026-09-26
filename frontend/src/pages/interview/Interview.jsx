import { useEffect, useRef, useState } from 'react';
import { createInterview, getInterviewDocuments, submitInterviewAnswer } from '../../api/interviewApi';
import InterviewCustom from './components/InterviewCustom';
import InterviewQuestion from './components/InterviewQuestion';
import InterviewResult from './components/InterviewResult';
import './interview.css';

const INTERVIEW_STEP = {
    CUSTOM: 'custom',
    QUESTION: 'question',
    RESULT: 'result',
};

const INITIAL_SETTINGS = {
    resumeNum: '',
    portfolioNum: '',
    letterNum: '',
    interviewStyle: '',
    difficulty: '',
};

const STEP_ORDER = {
    [INTERVIEW_STEP.CUSTOM]: 1,
    [INTERVIEW_STEP.QUESTION]: 2,
    [INTERVIEW_STEP.RESULT]: 3,
};

function createRequestId() {
    if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function Interview() {
    const [step, setStep] = useState(INTERVIEW_STEP.CUSTOM);
    const [settings, setSettings] = useState(INITIAL_SETTINGS);
    const [historyNum, setHistoryNum] = useState(null);
    const [questions, setQuestions] = useState([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [answers, setAnswers] = useState([]);
    const [answerTimeLimitSeconds, setAnswerTimeLimitSeconds] = useState(120);
    const [result, setResult] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [documents, setDocuments] = useState(null);
    const [documentsLoading, setDocumentsLoading] = useState(true);
    const startingRef = useRef(false);
    const startRequestIdRef = useRef(null);
    const [restartRequired, setRestartRequired] = useState(false);
    const [answerLocked, setAnswerLocked] = useState(false);

    useEffect(() => {
        let active = true;
        getInterviewDocuments()
            .then((data) => { if (active) setDocuments(data); })
            .catch(() => { if (active) setErrorMessage('저장된 서류를 불러오지 못했습니다. 로그인 상태를 확인해 주세요.'); })
            .finally(() => { if (active) setDocumentsLoading(false); });
        return () => { active = false; };
    }, []);

    const handleSettingChange = (name, value) => {
        if (startingRef.current) return;
        startRequestIdRef.current = null;
        setSettings((previousSettings) => ({
            ...previousSettings,
            [name]: value,
        }));
    };

    const handleStart = async () => {
        if (startingRef.current) {
            return;
        }
        if (!documents?.resumeList?.some((resume) => String(resume.documentNum) === settings.resumeNum)) {
            setErrorMessage('면접에 사용할 이력서를 선택해 주세요.');
            return;
        }
        if (!documents?.coverLetterList?.some(
            (coverLetter) => String(coverLetter.documentNum) === settings.letterNum,
        )) {
            setErrorMessage('면접에 사용할 자기소개서를 선택해 주세요.');
            return;
        }
        startingRef.current = true;
        setIsLoading(true);
        setErrorMessage('');

        try {
            startRequestIdRef.current ??= createRequestId();
            const interviewSession = await createInterview(settings, startRequestIdRef.current);

            setHistoryNum(interviewSession.historyNum);
            setQuestions(interviewSession.questions);
            setAnswerTimeLimitSeconds(interviewSession.answerTimeLimitSeconds);
            setCurrentQuestionIndex(0);
            setAnswers([]);
            setResult(null);
            setRestartRequired(false);
            setAnswerLocked(false);
            setStep(INTERVIEW_STEP.QUESTION);
        } catch (error) {
            const detail = error.response?.data?.data;
            setErrorMessage(typeof detail === 'string' ? detail : detail?.message
                || '면접 준비 응답을 받지 못했습니다. 같은 설정으로 다시 시도해 주세요.');
            if (error.response) startRequestIdRef.current = null;
        } finally {
            startingRef.current = false;
            setIsLoading(false);
        }
    };

    const handleAnswerSubmit = async (answer, timedOut = false) => {
        if (restartRequired) throw new Error('INTERVIEW_RESTART_REQUIRED');
        const currentQuestion = questions[currentQuestionIndex];
        setIsLoading(true);
        setErrorMessage('');

        try {
            const response = await submitInterviewAnswer(historyNum, {
                questionOrder: currentQuestion.questionOrder,
                answer,
                timedOut,
            });
            setAnswerLocked(false);

            setAnswers((previousAnswers) => [
                ...previousAnswers,
                {
                    questionOrder: currentQuestion.questionOrder,
                    answer,
                    timedOut,
                },
            ]);

            if (response.completed) {
                setResult(response.result);
                setStep(INTERVIEW_STEP.RESULT);
                return;
            }

            if (response.nextQuestion) {
                setQuestions((previousQuestions) => {
                    const questionExists = previousQuestions.some(
                        (question) => question.questionOrder === response.nextQuestion.questionOrder,
                    );

                    return questionExists
                        ? previousQuestions
                        : [...previousQuestions, response.nextQuestion];
                });
                setCurrentQuestionIndex((previousIndex) => previousIndex + 1);
            }
        } catch (error) {
            const detail = error.response?.data?.data;
            setRestartRequired(detail?.restartRequired === true);
            setAnswerLocked(detail?.answerLocked === true || !error.response);
            setErrorMessage(typeof detail === 'string' ? detail : detail?.message
                || '답변 처리 응답을 받지 못했습니다. 같은 답변으로 다시 제출해 주세요.');
            throw new Error('ANSWER_SUBMIT_FAILED', { cause: error });
        } finally {
            setIsLoading(false);
        }
    };

    const handleRestart = () => {
        setStep(INTERVIEW_STEP.CUSTOM);
        setSettings(INITIAL_SETTINGS);
        setHistoryNum(null);
        setQuestions([]);
        setCurrentQuestionIndex(0);
        setAnswers([]);
        setAnswerTimeLimitSeconds(120);
        setResult(null);
        setErrorMessage('');
        setRestartRequired(false);
        setAnswerLocked(false);
        startRequestIdRef.current = null;
    };

    const getStepClassName = (targetStep) => {
        if (step === targetStep) {
            return 'is-active';
        }

        return STEP_ORDER[step] > STEP_ORDER[targetStep] ? 'is-complete' : '';
    };

    return (
        <div className="interview-page">
            <p>AI MOCK INTERVIEW</p>
            <h1><b>모의면접</b></h1>
            <br />

            <div className="interview-explain">
                <span>1. 질문은 마이페이지에서 업로드한 유저의 서류 기반 질문 3개와 후속질문 2개로 구성되어있습니다.</span><br />
                <span>2. 유저는 면접 스타일과 제출할 문서를 지정하여 면접을 진행합니다.</span><br />
                <span>3. 면접이 종료되면 유저는 AI로부터 점수와 총평을 받을 수 있고 마이페이지에 기록됩니다.</span>
            </div>

            <div className="interview-container">
                <div className="interview-contents">
                    <div className="interview-explain-time"><span>약 15분 소요</span></div>

                    <ol className="interview-step-list">
                        <li className={getStepClassName(INTERVIEW_STEP.CUSTOM)}>문서 옵션</li>
                        <li className={getStepClassName(INTERVIEW_STEP.QUESTION)}>질의 응답</li>
                        <li className={getStepClassName(INTERVIEW_STEP.RESULT)}>결과 해설</li>
                    </ol>

                    <hr />
                    <p className="interview-analysis-title">분석 항목</p>
                    <div className="interview-analysis-list">
                        <span>자신감</span>
                        <span>끈기/열정</span>
                        <span>전문성</span>
                        <span>논리력</span>
                        <span>전달력</span>
                    </div>
                </div>

                <div className="interview-progress">
                    {errorMessage && <div className="interview-error" role="alert">
                        <p>{errorMessage}</p>
                        {restartRequired && <button type="button" className="btn btn-primary" onClick={handleRestart}>
                            새 면접 설정하기
                        </button>}
                    </div>}

                    {step === INTERVIEW_STEP.CUSTOM && (
                        <InterviewCustom
                            settings={settings}
                            documents={documents}
                            isLoading={isLoading || documentsLoading}
                            onSettingChange={handleSettingChange}
                            onStart={handleStart}
                        />
                    )}

                    {step === INTERVIEW_STEP.QUESTION && questions[currentQuestionIndex] && (
                        <InterviewQuestion
                            key={questions[currentQuestionIndex].questionOrder}
                            question={questions[currentQuestionIndex]}
                            currentQuestionIndex={currentQuestionIndex}
                            totalQuestions={5}
                            settings={settings}
                            isSubmitting={isLoading}
                            restartRequired={restartRequired}
                            answerLocked={answerLocked}
                            submittedAnswerCount={answers.length}
                            timeLimitSeconds={answerTimeLimitSeconds}
                            onSubmit={handleAnswerSubmit}
                        />
                    )}

                    {step === INTERVIEW_STEP.RESULT && result && (
                        <InterviewResult result={result} settings={settings} onRestart={handleRestart} />
                    )}
                </div>
            </div>
        </div>
    );
}

export default Interview;

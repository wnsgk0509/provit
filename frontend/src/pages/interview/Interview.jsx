import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { createInterview, discardInterviewSession, getInterviewDocuments,
    getInterviewSession, submitInterviewAnswer } from '../../api/interviewApi';
import { useAuth } from '../../context/AuthContext';
import { SCORE_ITEMS } from '../../constants/interviewEvaluation';
import { clearInterviewProgress, getClientDeadline, getResumeDraft,
    readInterviewProgress, saveInterviewProgress } from './interviewProgress';
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
    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuth();
    const userNum = user.userNum;
    const [recruitment, setRecruitment] = useState(() => location.state?.recruitment ?? null);
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
    const [savedProgress, setSavedProgress] = useState(() => readInterviewProgress(userNum));
    const initialProgressRef = useRef(savedProgress);
    const [resumeSession, setResumeSession] = useState(null);
    const [progressLoading, setProgressLoading] = useState(true);
    const [answerDraft, setAnswerDraft] = useState('');
    const [answerDeadline, setAnswerDeadline] = useState(null);
    const [expiresAt, setExpiresAt] = useState(null);

    useEffect(() => {
        let active = true;
        getInterviewDocuments()
            .then((data) => { if (active) setDocuments(data); })
            .catch(() => { if (active) setErrorMessage('저장된 서류를 불러오지 못했습니다. 로그인 상태를 확인해 주세요.'); })
            .finally(() => { if (active) setDocumentsLoading(false); });
        return () => { active = false; };
    }, []);

    useEffect(() => {
        let active = true;
        const progress = initialProgressRef.current;
        const checkProgress = async () => {
            try {
                if (progress) {
                    const session = await getInterviewSession(progress.historyNum);
                    if (active) setResumeSession(session);
                }
            } catch (error) {
                if (active && error.response?.data?.data?.restartRequired) {
                    clearInterviewProgress(userNum);
                    setSavedProgress(null);
                    setErrorMessage('이전 면접이 만료되었거나 서버가 재시작되어 새 면접이 필요합니다.');
                } else if (active) {
                    setErrorMessage('이전 면접을 확인하지 못했습니다. 이어가기를 눌러 다시 시도해 주세요.');
                }
            } finally { if (active) setProgressLoading(false); }
        };
        void checkProgress();
        return () => { active = false; };
    }, [userNum]);

    const persistProgress = (id, expiry, order, answer, locked = false) => {
        // 점검 직전 요청의 늦은 응답으로 만료된 초안이 다시 저장되지 않게 한다.
        if (expiry <= Date.now()) {
            clearInterviewProgress(userNum);
            setSavedProgress(null);
            return;
        }
        const progress = { historyNum: id, expiresAt: expiry, questionOrder: order, answer, answerLocked: locked };
        if (!saveInterviewProgress(userNum, progress)) {
            setErrorMessage('브라우저 저장소를 사용할 수 없어 작성 중인 답변을 자동 저장하지 못했습니다.');
        }
        setSavedProgress(progress);
    };

    const removeProgress = () => {
        clearInterviewProgress(userNum);
        setSavedProgress(null);
        setResumeSession(null);
    };

    const handleResume = async () => {
        if (!savedProgress || startingRef.current) return;
        startingRef.current = true;
        setIsLoading(true);
        setErrorMessage('');
        try {
            const session = await getInterviewSession(savedProgress.historyNum);
            const draft = getResumeDraft(readInterviewProgress(userNum), session);
            setSettings({
                resumeNum: String(session.settings.resumeNum),
                portfolioNum: session.settings.portfolioNum ? String(session.settings.portfolioNum) : '',
                letterNum: String(session.settings.letterNum),
                difficulty: session.settings.interviewDifficulty,
            });
            setRecruitment(session.settings.recruitment ?? null);
            setHistoryNum(session.historyNum);
            setQuestions(session.questions);
            setAnswers(session.answers);
            setCurrentQuestionIndex(session.answers.length);
            setAnswerTimeLimitSeconds(session.answerTimeLimitSeconds);
            setAnswerDeadline(getClientDeadline(session));
            setExpiresAt(session.expiresAt);
            setRestartRequired(false);
            setAnswerLocked(draft.answerLocked);
            setAnswerDraft(draft.answer);
            if (session.completed) {
                setResult(session.result);
                setStep(INTERVIEW_STEP.RESULT);
                removeProgress();
            } else {
                persistProgress(session.historyNum, session.expiresAt, session.answers.length + 1,
                    draft.answer, draft.answerLocked);
                setStep(INTERVIEW_STEP.QUESTION);
            }
        } catch (error) {
            const detail = error.response?.data?.data;
            if (detail?.restartRequired) removeProgress();
            setErrorMessage(detail?.message || '이전 면접을 불러오지 못했습니다. 다시 시도해 주세요.');
        } finally {
            startingRef.current = false;
            setIsLoading(false);
        }
    };

    const handleDiscardProgress = async () => {
        if (!savedProgress || startingRef.current) return;
        startingRef.current = true;
        setIsLoading(true);
        try {
            await discardInterviewSession(savedProgress.historyNum);
            removeProgress();
            setErrorMessage('');
            startRequestIdRef.current = null;
        } catch { setErrorMessage('이전 면접을 정리하지 못했습니다. 다시 시도해 주세요.'); }
        finally { startingRef.current = false; setIsLoading(false); }
    };

    const handleSettingChange = (name, value) => {
        if (startingRef.current) return;
        startRequestIdRef.current = null;
        setSettings((previousSettings) => ({
            ...previousSettings,
            [name]: value,
        }));
    };

    const handleStart = async () => {
        if (startingRef.current || savedProgress || progressLoading) {
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
            const interviewSession = await createInterview(settings, startRequestIdRef.current, recruitment);

            setHistoryNum(interviewSession.historyNum);
            setQuestions(interviewSession.questions);
            setAnswerTimeLimitSeconds(interviewSession.answerTimeLimitSeconds);
            setAnswerDeadline(getClientDeadline(interviewSession));
            setExpiresAt(interviewSession.expiresAt);
            setAnswerDraft('');
            persistProgress(interviewSession.historyNum, interviewSession.expiresAt, 1, '');
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
        persistProgress(historyNum, expiresAt, currentQuestion.questionOrder, answer, true);

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
                removeProgress();
                setResult(response.result);
                setStep(INTERVIEW_STEP.RESULT);
                return;
            }

            if (response.nextQuestion) {
                setAnswerDraft('');
                setAnswerDeadline(getClientDeadline(response));
                setExpiresAt(response.expiresAt);
                persistProgress(historyNum, response.expiresAt, response.nextQuestion.questionOrder, '');
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
            if (detail?.restartRequired) removeProgress();
            else persistProgress(historyNum, expiresAt, currentQuestion.questionOrder, answer,
                detail?.answerLocked === true || !error.response);
            setErrorMessage(typeof detail === 'string' ? detail : detail?.message
                || '답변 처리 응답을 받지 못했습니다. 같은 답변으로 다시 제출해 주세요.');
            throw new Error('ANSWER_SUBMIT_FAILED', { cause: error });
        } finally {
            setIsLoading(false);
        }
    };

    const handleRestart = () => {
        removeProgress();
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
        setAnswerDraft('');
        setAnswerDeadline(null);
        setExpiresAt(null);
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
                <span>1. 질문은 마이페이지에서 업로드한 유저의 서류 기반 질문과 선택한 직무 역량 질문으로 총 5문항 구성되어있습니다.</span><br />
                <span>2. 유저는 제출할 문서와 난이도를 선택하고 AI 면접관과 면접을 진행합니다.</span><br />
                <span>3. 면접이 종료되면 점수와 총평을 받고 해당 정보는 마이페이지에 기록됩니다.</span>
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
                        {SCORE_ITEMS.map((item) => <span key={item.key} title={item.description}>{item.label}</span>)}
                    </div>
                </div>

                <div className="interview-progress">
                    {step === INTERVIEW_STEP.CUSTOM && savedProgress && <section className="interview-recruitment">
                        <h2>{resumeSession?.completed ? '완료된 면접이 있습니다' : '진행 중인 면접이 있습니다'}</h2>
                        <p>{resumeSession?.settings?.recruitment?.companyName || '저장된 모의면접'} · 오늘 23:55까지 보관됩니다.</p>
                        <p>이어가면 기존 문서·공고·질문을 사용합니다. 답변 제한 시간은 종료 중에도 흐릅니다.</p>
                        <div className="d-flex flex-wrap gap-2">
                            <button type="button" className="btn btn-primary" onClick={handleResume}
                                disabled={isLoading || progressLoading}>
                                {resumeSession?.completed ? '면접 결과 확인' : '이전 면접 이어가기'}
                            </button>
                            <button type="button" className="btn btn-outline-secondary" onClick={handleDiscardProgress}
                                disabled={isLoading || progressLoading}>이전 면접 종료하고 새로 시작</button>
                        </div>
                    </section>}
                    {recruitment && <section className="interview-recruitment" aria-label="선택한 채용 공고">
                        <span>지원 공고</span>
                        <h2>{recruitment.companyName}</h2>
                        <p>{recruitment.title}</p>
                        <p>{[recruitment.jobName, recruitment.locationName, recruitment.experienceLevel]
                            .filter(Boolean).join(' · ')}</p>
                        <small>이 공고를 바탕으로 면접 질문과 평가가 진행됩니다.</small>
                    </section>}
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
                            isLoading={isLoading || documentsLoading || progressLoading}
                            startDisabled={Boolean(savedProgress)}
                            onSettingChange={handleSettingChange}
                            onStart={handleStart}
                        />
                    )}

                    {step === INTERVIEW_STEP.QUESTION && questions[currentQuestionIndex] && (
                        <InterviewQuestion
                            key={`${historyNum}:${questions[currentQuestionIndex].questionOrder}`}
                            question={questions[currentQuestionIndex]}
                            currentQuestionIndex={currentQuestionIndex}
                            totalQuestions={5}
                            settings={settings}
                            isSubmitting={isLoading}
                            restartRequired={restartRequired}
                            answerLocked={answerLocked}
                            submittedAnswerCount={answers.length}
                            timeLimitSeconds={answerTimeLimitSeconds}
                            answerDeadline={answerDeadline}
                            initialAnswer={answerDraft}
                            onAnswerChange={(answer) => {
                                setAnswerDraft(answer);
                                persistProgress(historyNum, expiresAt, questions[currentQuestionIndex].questionOrder, answer);
                            }}
                            onPause={() => navigate('/')}
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

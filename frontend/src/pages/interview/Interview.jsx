import { useEffect, useState } from 'react';
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

    useEffect(() => {
        let active = true;
        getInterviewDocuments()
            .then((data) => { if (active) setDocuments(data); })
            .catch(() => { if (active) setErrorMessage('저장된 서류를 불러오지 못했습니다. 로그인 상태를 확인해 주세요.'); })
            .finally(() => { if (active) setDocumentsLoading(false); });
        return () => { active = false; };
    }, []);

    const handleSettingChange = (name, value) => {
        setSettings((previousSettings) => ({
            ...previousSettings,
            [name]: value,
        }));
    };

    const handleStart = async () => {
        if (!documents?.resumeList?.some((resume) => String(resume.resumeNum) === settings.resumeNum)) {
            setErrorMessage('면접에 사용할 이력서를 선택해 주세요.');
            return;
        }
        setIsLoading(true);
        setErrorMessage('');

        try {
            const interviewSession = await createInterview(settings);

            setHistoryNum(interviewSession.historyNum);
            setQuestions(interviewSession.questions);
            setAnswerTimeLimitSeconds(interviewSession.answerTimeLimitSeconds);
            setCurrentQuestionIndex(0);
            setAnswers([]);
            setResult(null);
            setStep(INTERVIEW_STEP.QUESTION);
        } catch {
            setErrorMessage('면접을 준비하지 못했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAnswerSubmit = async (answer, timedOut = false) => {
        const currentQuestion = questions[currentQuestionIndex];
        setIsLoading(true);
        setErrorMessage('');

        try {
            const response = await submitInterviewAnswer(historyNum, {
                questionOrder: currentQuestion.questionOrder,
                answer,
                timedOut,
            });

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
        } catch {
            setErrorMessage('답변을 제출하지 못했습니다. 입력한 답변은 유지되므로 다시 시도해 주세요.');
            throw new Error('ANSWER_SUBMIT_FAILED');
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
                    {errorMessage && <div className="interview-error" role="alert">{errorMessage}</div>}

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

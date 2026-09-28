import { useCallback, useEffect, useRef, useState } from 'react';
import { remainingAnswerSeconds } from '../interviewProgress';
import { DIFFICULTY_NAMES } from '../../../constants/interviewDifficulty';

const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
};

function InterviewQuestion({
    question,
    currentQuestionIndex,
    totalQuestions,
    settings,
    isSubmitting,
    restartRequired,
    answerLocked,
    submittedAnswerCount,
    timeLimitSeconds,
    answerDeadline,
    initialAnswer,
    onAnswerChange,
    onPause,
    onSubmit,
}) {
    const [answer, setAnswer] = useState(initialAnswer ?? '');
    const [timeLeft, setTimeLeft] = useState(() => remainingAnswerSeconds(answerDeadline));
    const submittingRef = useRef(false);
    const timeoutSubmittedRef = useRef(answerLocked);
    const questionNumber = currentQuestionIndex + 1;
    const questionLabel = questionNumber === 1 ? '자기소개서 질문'
        : questionNumber === 2 ? (Number(settings.portfolioNum) > 0 ? '포트폴리오 질문' : '자기소개서 질문')
        : questionNumber === 3 ? '직무 지식 질문'
        : questionNumber === 4 ? '직무 문제해결 질문'
        : '꼬리질문';
    const isLastQuestion = questionNumber === totalQuestions;
    const questionProgress = (submittedAnswerCount / totalQuestions) * 100;
    const timerProgress = (timeLeft / timeLimitSeconds) * 100;
    const hasTimedOut = timeLeft === 0;

    const submitCurrentAnswer = useCallback(async (timedOut) => {
        const trimmedAnswer = answer.trim();

        if (submittingRef.current || isSubmitting || restartRequired || (!timedOut && !trimmedAnswer)) {
            return;
        }

        submittingRef.current = true;
        timeoutSubmittedRef.current = true;

        try {
            await onSubmit(answer, timedOut);
        } catch {
            submittingRef.current = false;
        }
    }, [answer, isSubmitting, restartRequired, onSubmit]);

    useEffect(() => {
        const updateTimeLeft = () => {
            setTimeLeft(remainingAnswerSeconds(answerDeadline));
        };

        const intervalId = window.setInterval(updateTimeLeft, 250);
        updateTimeLeft();

        return () => window.clearInterval(intervalId);
    }, [answerDeadline]);

    useEffect(() => {
        if (hasTimedOut && !timeoutSubmittedRef.current && !isSubmitting && !restartRequired) {
            timeoutSubmittedRef.current = true;
            void submitCurrentAnswer(true);
        }
    }, [hasTimedOut, isSubmitting, restartRequired, submitCurrentAnswer]);

    const handleAnswerChange = (event) => {
        setAnswer(event.target.value);
        onAnswerChange(event.target.value);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        void submitCurrentAnswer(hasTimedOut);
    };

    const timerClassName = [
        'interview-timer',
        timeLeft <= 30 ? 'is-warning' : '',
        timeLeft <= 10 ? 'is-critical' : '',
    ].filter(Boolean).join(' ');

    return (
        <section className="interview-question">
            <div className="interview-question-top">
                <span className={`interview-question-type ${question.questionType.toLowerCase()}`}>
                    {questionLabel}
                </span>
                <span className="interview-question-count">{questionNumber} / {totalQuestions}</span>
            </div>

            <div
                className="interview-progress-bar"
                role="progressbar"
                aria-label="면접 진행률"
                aria-valuemin="0"
                aria-valuemax="100"
                aria-valuenow={questionProgress}
            >
                <span style={{ width: `${questionProgress}%` }} />
            </div>

            <div className={timerClassName}>
                <div className="interview-timer-heading">
                    <span>{hasTimedOut ? '제한시간 종료' : '남은 답변 시간'}</span>
                    <strong aria-live="polite">{formatTime(timeLeft)}</strong>
                </div>
                <div
                    className="interview-timer-bar"
                    role="progressbar"
                    aria-label="남은 답변 시간"
                    aria-valuemin="0"
                    aria-valuemax={timeLimitSeconds}
                    aria-valuenow={timeLeft}
                >
                    <span style={{ width: `${timerProgress}%` }} />
                </div>
            </div>

            <div className="interview-setting-summary">
                <span>일대일면접</span>
                <span>{DIFFICULTY_NAMES[settings.difficulty]}</span>
            </div>

            <div className="interview-question-card">
                <span>Q{questionNumber}</span>
                <h2>{question.questionText}</h2>
            </div>

            <form onSubmit={handleSubmit}>
                <label htmlFor="interview-answer">답변</label>
                <textarea
                    id="interview-answer"
                    value={answer}
                    onChange={handleAnswerChange}
                    placeholder="답변을 구체적으로 작성해 주세요."
                    maxLength="1000"
                    disabled={isSubmitting || hasTimedOut || restartRequired || answerLocked}
                    required={!hasTimedOut}
                />
                <div className="interview-answer-meta">
                    <span>
                        {answerLocked
                            ? '처리한 답변이 달라지지 않도록 입력을 유지합니다. 같은 답변으로 다시 제출해 주세요.'
                            : hasTimedOut
                            ? '제한시간이 종료되었습니다. 제출에 실패했다면 다시 제출해 주세요.'
                            : '상황, 행동, 결과를 포함하면 더 정확한 평가를 받을 수 있습니다.'}
                    </span>
                    <span>{answer.length} / 1000</span>
                </div>

                <button
                    className="btn btn-primary interview-primary-button"
                    type="submit"
                    disabled={(!answer.trim() && !hasTimedOut) || isSubmitting || restartRequired}
                >
                    {isSubmitting
                        ? '답변 제출 중...'
                        : hasTimedOut
                            ? '시간 초과 답변 다시 제출'
                            : isLastQuestion ? '답변 제출 및 결과 보기' : '답변 제출'}
                </button>
                <button className="btn btn-outline-secondary" type="button" onClick={onPause}
                    disabled={isSubmitting}>저장하고 나가기</button>
                <p className="small text-muted mt-2 mb-0">
                    작성 중인 답변은 자동 저장됩니다. 답변 제한 시간은 나간 뒤에도 흐르며,
                    이어가기는 오늘 23:55까지 가능합니다.
                </p>
            </form>
        </section>
    );
}

export default InterviewQuestion;

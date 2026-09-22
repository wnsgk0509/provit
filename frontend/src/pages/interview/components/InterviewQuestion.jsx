import { useCallback, useEffect, useRef, useState } from 'react';

const STYLE_NAMES = {
    RANDOM: '랜덤면접',
    ONE_TO_ONE: '일대일면접',
    PANEL: '다대일면접',
    GROUP: '다대다면접',
};

const DIFFICULTY_NAMES = {
    HARD: '압박면접',
    NORMAL: '심층면접',
    EASY: '일반면접',
};

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
    submittedAnswerCount,
    timeLimitSeconds,
    onSubmit,
}) {
    const [answer, setAnswer] = useState('');
    const [timeLeft, setTimeLeft] = useState(timeLimitSeconds);
    const submittingRef = useRef(false);
    const questionNumber = currentQuestionIndex + 1;
    const isLastQuestion = questionNumber === totalQuestions;
    const questionProgress = (submittedAnswerCount / totalQuestions) * 100;
    const timerProgress = (timeLeft / timeLimitSeconds) * 100;
    const hasTimedOut = timeLeft === 0;

    const submitCurrentAnswer = useCallback(async (timedOut) => {
        const trimmedAnswer = answer.trim();

        if (submittingRef.current || isSubmitting || (!timedOut && !trimmedAnswer)) {
            return;
        }

        submittingRef.current = true;

        try {
            await onSubmit(answer, timedOut);
        } catch {
            submittingRef.current = false;
        }
    }, [answer, isSubmitting, onSubmit]);

    useEffect(() => {
        const deadline = Date.now() + timeLimitSeconds * 1000;

        const updateTimeLeft = () => {
            const remainingMilliseconds = deadline - Date.now();
            const remainingSeconds = Math.max(0, Math.ceil(remainingMilliseconds / 1000));
            setTimeLeft(remainingSeconds);
        };

        const intervalId = window.setInterval(updateTimeLeft, 250);
        updateTimeLeft();

        return () => window.clearInterval(intervalId);
    }, [timeLimitSeconds]);

    useEffect(() => {
        if (hasTimedOut) {
            void submitCurrentAnswer(true);
        }
    }, [hasTimedOut, submitCurrentAnswer]);

    const handleAnswerChange = (event) => {
        setAnswer(event.target.value);
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
                    {question.questionType === 'DOCUMENT' ? '서류 질문' : '후속 질문'}
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
                <span>{STYLE_NAMES[settings.interviewStyle]}</span>
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
                    disabled={isSubmitting || hasTimedOut}
                    required={!hasTimedOut}
                />
                <div className="interview-answer-meta">
                    <span>
                        {hasTimedOut
                            ? '제한시간이 종료되어 현재 답변을 제출하고 있습니다.'
                            : '상황, 행동, 결과를 포함하면 더 정확한 평가를 받을 수 있습니다.'}
                    </span>
                    <span>{answer.length} / 1000</span>
                </div>

                <button
                    className="btn btn-primary interview-primary-button"
                    type="submit"
                    disabled={(!answer.trim() && !hasTimedOut) || isSubmitting}
                >
                    {isSubmitting
                        ? '답변 제출 중...'
                        : hasTimedOut
                            ? '시간 초과 답변 다시 제출'
                            : isLastQuestion ? '답변 제출 및 결과 보기' : '답변 제출'}
                </button>
            </form>
        </section>
    );
}

export default InterviewQuestion;

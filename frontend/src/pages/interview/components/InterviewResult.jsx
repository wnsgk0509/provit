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

function InterviewResult({ result, onRestart }) {
    return (
        <section className="interview-result">
            <div className="interview-result-heading">
                <div>
                    <span>INTERVIEW COMPLETE</span>
                    <h2>면접 결과</h2>
                    <p>{STYLE_NAMES[result.interviewStyle]} · {DIFFICULTY_NAMES[result.difficulty]}</p>
                </div>
                <div className="interview-total-score">
                    <strong>{result.totalScore}</strong>
                    <span>/ 100점</span>
                </div>
            </div>

            <div className="interview-score-list">
                {result.scores.map((scoreItem) => (
                    <div className="interview-score-item" key={scoreItem.category}>
                        <div>
                            <span>{scoreItem.label}</span>
                            <strong>{scoreItem.score}점</strong>
                        </div>
                        <div className="interview-score-bar">
                            <span style={{ width: `${scoreItem.score}%` }} />
                        </div>
                    </div>
                ))}
            </div>

            <div className="interview-feedback-grid">
                <article>
                    <span className="interview-feedback-label strength">잘한 점</span>
                    <p>{result.feedback.strengths}</p>
                </article>
                <article>
                    <span className="interview-feedback-label weakness">아쉬운 점</span>
                    <p>{result.feedback.weaknesses}</p>
                </article>
                <article>
                    <span className="interview-feedback-label comparison">이전 기록 비교</span>
                    <p>{result.feedback.comparison}</p>
                </article>
                <article>
                    <span className="interview-feedback-label improvement">개선할 점</span>
                    <p>{result.feedback.improvements}</p>
                </article>
            </div>

            <button className="btn btn-outline-primary interview-restart-button" type="button" onClick={onRestart}>
                새로운 면접 시작하기
            </button>
        </section>
    );
}

export default InterviewResult;

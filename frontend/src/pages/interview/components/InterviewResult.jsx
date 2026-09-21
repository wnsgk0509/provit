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

const SCORE_ITEMS = [
    { key: 'confidenceScore', category: 'CONFIDENCE', label: '자신감' },
    { key: 'persistenceScore', category: 'PERSISTENCE', label: '끈기/열정' },
    { key: 'expertiseScore', category: 'EXPERTISE', label: '전문성' },
    { key: 'logicScore', category: 'LOGIC', label: '논리력' },
    { key: 'deliveryScore', category: 'DELIVERY', label: '전달력' },
];

function InterviewResult({ result, settings, onRestart }) {
    return (
        <section className="interview-result">
            <div className="interview-result-heading">
                <div>
                    <span>INTERVIEW COMPLETE</span>
                    <h2>면접 결과</h2>
                    <p>{STYLE_NAMES[settings.interviewStyle]} · {DIFFICULTY_NAMES[settings.difficulty]}</p>
                </div>
                <div className="interview-total-score">
                    <strong>{result.totalScore}</strong>
                    <span>/ 100점</span>
                </div>
            </div>

            <div className="interview-score-list">
                {SCORE_ITEMS.map((scoreItem) => (
                    <div className="interview-score-item" key={scoreItem.category}>
                        <div>
                            <span>{scoreItem.label}</span>
                            <strong>{result[scoreItem.key]}점</strong>
                        </div>
                        <div className="interview-score-bar">
                            <span style={{ width: `${result[scoreItem.key]}%` }} />
                        </div>
                    </div>
                ))}
            </div>

            <div className="interview-feedback-grid">
                <article>
                    <span className="interview-feedback-label strength">잘한 점</span>
                    <p>{result.strengths}</p>
                </article>
                <article>
                    <span className="interview-feedback-label weakness">아쉬운 점</span>
                    <p>{result.weaknesses}</p>
                </article>
                <article>
                    <span className="interview-feedback-label comparison">이전 기록 비교</span>
                    <p>{result.comparison}</p>
                </article>
                <article>
                    <span className="interview-feedback-label improvement">개선할 점</span>
                    <p>{result.improvements}</p>
                </article>
            </div>

            <button className="btn btn-outline-primary interview-restart-button" type="button" onClick={onRestart}>
                새로운 면접 시작하기
            </button>
        </section>
    );
}

export default InterviewResult;

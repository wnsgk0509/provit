const categoryLabels = {
    experience: '경험',
    skill: '기술',
    certification: '자격증',
    qualification: '스펙·결과물',
};

function CareerPreparation({ preparation, isExample }) {
    return (
        <section className="review-career-preparation" aria-labelledby="review-preparation-title">
            <div className="review-preparation-heading">
                <div>
                    <h3 id="review-preparation-title">직무별 취업 준비 추천</h3>
                    <p>문장 첨삭과 별도로, 지원 분야에 맞는 경험·기술·자격증·스펙을 준비해 보세요.</p>
                </div>
                {isExample && <span className="review-example-badge">추천 예시</span>}
            </div>
            {preparation ? (
                <>
                    <p className="review-preparation-target">
                        지원 분야 · {[preparation.occupationName, preparation.jobName].filter(Boolean).join(' / ') || '미입력'}
                    </p>
                    <p className="review-preparation-summary">{preparation.summary}</p>
                    {preparation.coverageNote && <p className="review-field-hint">{preparation.coverageNote}</p>}
                    {preparation.recommendations.length > 0 && (
                        <div className="review-preparation-list">
                            {preparation.recommendations.map((item, index) => (
                                <article className="review-preparation-item" key={`${item.category}:${item.title}:${index}`}>
                                    <span className="review-section-label">{categoryLabels[item.category] || '준비 항목'}</span>
                                    <h4>{item.title}</h4>
                                    <p>{item.reason}</p>
                                    <div className="review-preparation-action">
                                        <strong>준비 방법</strong>
                                        <p>{item.action}</p>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </>
            ) : (
                <p className="review-field-hint">이 기록에는 취업 준비 추천이 저장되어 있지 않습니다. 새 첨삭 기록에서 확인할 수 있습니다.</p>
            )}
        </section>
    );
}

export default CareerPreparation;

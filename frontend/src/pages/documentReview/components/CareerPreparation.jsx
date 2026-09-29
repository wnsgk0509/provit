import ReviewEvidence from './ReviewEvidence';

const categoryLabels = {
    experience: '실무·프로젝트 경험',
    skill: '기술 역량',
    certification: '자격증',
    qualification: '기타 객관적 스펙',
};

function CareerPreparation({ preparation, isExample }) {
    return (
        <section className="review-career-preparation" aria-labelledby="review-preparation-title">
            <div className="review-preparation-heading">
                <div>
                    <h3 id="review-preparation-title">직무별 취업 준비 보강 항목</h3>
                    <p>서류에서 확인되지 않은 경험·기술·자격증·기타 객관적 스펙의 준비 방향입니다. 이미 보유했다면 실제 근거를 서류에 보완해 주세요.</p>
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
                                    <ReviewEvidence sources={item.sources} heading="추천 판단의 배경 원문" />
                                    <p className="review-reason"><strong>보강 판단 이유</strong>{item.reason}</p>
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

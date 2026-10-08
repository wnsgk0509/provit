const documentLabels = { resume: '이력서', 'cover-letter': '자기소개서', portfolio: '포트폴리오' };

function ReviewEvidence({ sources, heading = '판단 근거 원문' }) {
    if (!sources?.length) return null;
    return (
        <div className="review-evidence">
            {heading && <p className="review-evidence-heading">{heading}</p>}
            <div className="review-consistency-sources">
                {sources.map((source, index) => (
                    <section key={`${source.documentType}:${source.section}:${index}`}>
                        <strong>
                            {documentLabels[source.documentType] || source.documentType} · {source.section}
                            {source.pageNumber != null ? ` · PDF ${source.pageNumber}페이지` : ''}
                        </strong>
                        <p>{source.text}</p>
                    </section>
                ))}
            </div>
        </div>
    );
}

export default ReviewEvidence;

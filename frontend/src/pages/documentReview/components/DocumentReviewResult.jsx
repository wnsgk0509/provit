import { useState } from 'react';
import { CheckCircle2, ChevronDown, ClipboardCheck, Layers, LoaderCircle, Sparkles, X } from 'lucide-react';
import { createBundleReviewExample } from '../reviewExamples';

const DOCUMENT_RESULTS = [
    { key: 'resume', label: '이력서' },
    { key: 'coverLetter', label: '자기소개서' },
    { key: 'portfolio', label: '포트폴리오' },
];
const documentLabels = { resume: '이력서', 'cover-letter': '자기소개서', portfolio: '포트폴리오' };
const consistencyLabels = { mismatch: '내용 불일치', missingEvidence: '근거 보완', needsConfirmation: '확인 필요' };

function DocumentReviewResult({ includePortfolio, result, isSubmitting, error }) {
    const [showExample, setShowExample] = useState(false);
    const isExample = !result && showExample;
    const displayedResult = result || (isExample ? createBundleReviewExample(includePortfolio) : null);

    return (
        <section
            className="review-card review-result-card"
            aria-labelledby="review-result-title"
            aria-busy={isSubmitting}
        >
            <div className="review-card-heading">
                <span className="review-step">03</span>
                <div>
                    <h2 id="review-result-title">통합 첨삭 결과</h2>
                    <p>종합 피드백, 서류 간 일관성과 문서별 첨삭을 확인하세요.</p>
                </div>
                {isExample && <span className="review-example-badge">결과 예시</span>}
            </div>
            {isSubmitting ? (
                <div className="review-result-empty" role="status">
                    <LoaderCircle size={32} className="review-spinner" aria-hidden="true" />
                    <h3>선택한 서류를 함께 살펴보고 있습니다.</h3>
                    <p>통합 첨삭이 완료되면 결과가 여기에 표시됩니다.</p>
                </div>
            ) : error ? (
                <div className="review-status is-error" role="alert">
                    <p>{error}</p>
                </div>
            ) : displayedResult ? (
                <div className="review-result-content">
                    {isExample && (
                        <div className="review-example-notice" role="status">
                            <p>
                                화면 구성을 보여주는 예시입니다.
                                <br />
                                선택한 서류를 분석한 결과가 아닙니다.
                            </p>
                            <button type="button" onClick={() => setShowExample(false)} aria-label="결과 예시 닫기">
                                <X size={18} />
                            </button>
                        </div>
                    )}
                    <section className="review-summary">
                        <span>종합 피드백</span>
                        <p>{displayedResult.summary}</p>
                    </section>
                    <ReviewStrengths strengths={displayedResult.strengths} />
                    <section className="review-consistency-result" aria-labelledby="review-consistency-title">
                        <h3 id="review-consistency-title">
                            <Layers size={18} aria-hidden="true" /> 서류 간 일관성 확인
                        </h3>
                        {displayedResult.consistencyIssues.length === 0 ? (
                            <p className="review-field-hint">서류 간 확인이 필요한 사항이 없습니다.</p>
                        ) : (
                            displayedResult.consistencyIssues.map((issue, index) => (
                                <article className="review-consistency-issue" key={issue.title + ':' + index}>
                                    <span className="review-section-label">
                                        {consistencyLabels[issue.type] || '확인 필요'}
                                    </span>
                                    <h4>{issue.title}</h4>
                                    <div className="review-consistency-sources">
                                        {issue.sources.map((source, sourceIndex) => (
                                            <section
                                                key={source.documentType + ':' + source.section + ':' + sourceIndex}
                                            >
                                                <strong>
                                                    {documentLabels[source.documentType] || source.documentType} ·{' '}
                                                    {source.section}
                                                </strong>
                                                <p>{source.text}</p>
                                            </section>
                                        ))}
                                    </div>
                                    <p className="review-reason">
                                        <strong>확인·개선 방향</strong>
                                        {issue.recommendation}
                                    </p>
                                </article>
                            ))
                        )}
                    </section>
                    <div className="review-document-results">
                        {DOCUMENT_RESULTS.filter((document) => displayedResult.documentReviews[document.key]).map(
                            (document) => (
                                <details className="review-document-result" key={document.key}>
                                    <summary>
                                        <strong>{document.label} 첨삭</strong>
                                        <span>
                                            개선 제안{' '}
                                            {displayedResult.documentReviews[document.key].improvements.length}개{' '}
                                            <ChevronDown size={16} aria-hidden="true" />
                                        </span>
                                    </summary>
                                    <ReviewDocumentFeedback
                                        feedback={displayedResult.documentReviews[document.key]}
                                        isExample={isExample}
                                    />
                                </details>
                            ),
                        )}
                    </div>
                </div>
            ) : (
                <div className="review-result-empty">
                    <span className="review-result-icon">
                        <ClipboardCheck size={34} aria-hidden="true" />
                    </span>
                    <h3>내 서류가 하나의 이야기로 이어지도록</h3>
                    <p>
                        종합 피드백과 서류 간 일관성,
                        <br />각 문서의 수정 제안을 한 곳에서 확인하세요.
                    </p>
                    <button type="button" className="review-secondary-button" onClick={() => setShowExample(true)}>
                        통합 결과 예시 보기
                    </button>
                </div>
            )}
        </section>
    );
}

function ReviewStrengths({ strengths }) {
    return (
        <section className="review-strengths">
            <h3>
                <CheckCircle2 size={18} aria-hidden="true" /> 잘 드러난 강점
            </h3>
            <ul>
                {strengths.map((strength) => (
                    <li key={strength}>{strength}</li>
                ))}
            </ul>
        </section>
    );
}

function ReviewDocumentFeedback({ feedback, isExample }) {
    return (
        <div className="review-document-feedback">
            <p className="review-document-summary">{feedback.summary}</p>
            <ReviewStrengths strengths={feedback.strengths} />
            <div className="review-improvements">
                {feedback.improvements.map((item, index) => (
                    <article className="review-improvement" key={item.section + ':' + index}>
                        <span className="review-section-label">{item.section}</span>
                        <h3>{item.title}</h3>
                        <p className="review-issue">{item.issue}</p>
                        <div className="review-comparison">
                            <section>
                                <h4>원문{isExample ? ' 예시' : ''}</h4>
                                <p>{item.original}</p>
                            </section>
                            <section className="review-suggestion">
                                <h4>
                                    <Sparkles size={14} aria-hidden="true" /> 수정 제안
                                </h4>
                                <p>{item.suggestion}</p>
                            </section>
                        </div>
                        <p className="review-reason">
                            <strong>수정 이유</strong>
                            {item.reason}
                        </p>
                    </article>
                ))}
            </div>
        </div>
    );
}

export default DocumentReviewResult;

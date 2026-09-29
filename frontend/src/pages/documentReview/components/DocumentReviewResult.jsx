import { CheckCircle2, ChevronDown, ClipboardCheck, Layers, LoaderCircle, Sparkles, X } from 'lucide-react';
import { createBundleReviewExample } from '../reviewExamples';
import { REVIEW_MODE_OPTIONS } from '../documentReviewConfig';
import CareerPreparation from './CareerPreparation';

const DOCUMENT_RESULTS = [
    { key: 'resume', label: '이력서' },
    { key: 'coverLetter', label: '자기소개서' },
    { key: 'portfolio', label: '포트폴리오' },
];
const documentLabels = { resume: '이력서', 'cover-letter': '자기소개서', portfolio: '포트폴리오' };
const consistencyLabels = { mismatch: '내용 불일치', missingEvidence: '근거 보완', needsConfirmation: '확인 필요' };

function DocumentReviewResult({ includePortfolio, result, isSubmitting, error, showExample, onShowExample, onCloseExample }) {
    const isExample = !result && showExample;
    const displayedResult = result || (isExample ? createBundleReviewExample(includePortfolio) : null);
    const isDummy = result?.resultSource === 'DUMMY';
    const isIncomplete = result && result.reviewStatus !== 'COMPLETED';

    return (
        <section
            className="review-stage review-result-card"
            data-review-step="2"
            aria-labelledby="review-result-title"
            aria-busy={isSubmitting}
        >
            <div className="review-card-heading">
                <div>
                    <h2 id="review-result-title" tabIndex={-1}>통합 첨삭 결과</h2>
                    <p>종합 피드백, 서류 간 일관성과 문서별 첨삭을 확인하세요.</p>
                </div>
                {(isExample || isDummy) && <span className="review-example-badge">{isDummy ? '저장된 더미 결과' : '결과 예시'}</span>}
                {result?.resultSource === 'AI' && <span className="review-example-badge">AI 분석 결과</span>}
            </div>
            {isSubmitting ? (
                <div className="review-result-empty" role="status">
                    <LoaderCircle size={32} className="review-spinner" aria-hidden="true" />
                    <h3>AI 첨삭 결과를 기다리고 있습니다.</h3>
                    <p>서류 분석과 결과 저장에 최대 2분 정도 걸릴 수 있습니다.</p>
                </div>
            ) : error ? (
                <div className="review-status is-error" role="alert">
                    <p>{error}</p>
                </div>
            ) : displayedResult ? (
                <div className="review-result-content">
                    {isDummy && (
                        <div className="review-example-notice" role="status">
                            <p>DB에 저장된 예시 응답입니다. 아래 원문·피드백·비교 근거는 더미 데이터이며 실제 서류의 AI 분석 결과가 아닙니다.</p>
                        </div>
                    )}
                    {result && (
                        <section className="review-saved-request" aria-label="저장된 요청 정보">
                            <h3>요청 정보 · 기록 #{result.reviewNum}</h3>
                            <dl>
                                <div><dt>저장 시각</dt><dd>{result.finishedAt || result.createdAt}</dd></div>
                                <div><dt>첨삭 기준</dt><dd>{REVIEW_MODE_OPTIONS.find((mode) => mode.value === result.reviewMode)?.label}</dd></div>
                                {result.customCriteria && <div><dt>직접 입력 기준</dt><dd>{result.customCriteria}</dd></div>}
                                <div><dt>추가 요청</dt><dd>{result.instructions || '없음'}</dd></div>
                            </dl>
                            <ul>{result.documents.map((document) => (
                                <li key={document.reviewDocumentNum}>
                                    {documentLabels[document.documentType]} · {document.documentTitle}
                                    {document.originalFileName ? ` (${document.originalFileName})` : ''}
                                </li>
                            ))}</ul>
                        </section>
                    )}
                    {isExample && (
                        <div className="review-example-notice" role="status">
                            <p>
                                화면 구성을 보여주는 예시입니다.
                                <br />
                                선택한 서류를 분석한 결과가 아닙니다.
                            </p>
                            <button type="button" onClick={onCloseExample} aria-label="결과 예시 닫기">
                                <X size={18} />
                            </button>
                        </div>
                    )}
                    {isIncomplete ? (
                        <div className={`review-status${result.reviewStatus === 'FAILED' ? ' is-error' : ''}`} role="status">
                            <p>{result.reviewStatus === 'FAILED' ? result.errorMessage || '첨삭 처리에 실패했습니다.' : '첨삭 처리 중입니다. 잠시 후 기록을 다시 열어 주세요.'}</p>
                        </div>
                    ) : <>
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
                                        isExample={isExample || isDummy}
                                    />
                                </details>
                            ),
                        )}
                    </div>
                    <CareerPreparation
                        preparation={displayedResult.careerPreparation}
                        isExample={isExample || isDummy}
                    />
                    </>}
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
                    <button type="button" className="review-secondary-button" onClick={onShowExample}>
                        통합 결과 예시 보기
                    </button>
                </div>
            )}
        </section>
    );
}

function ReviewStrengths({ strengths }) {
    if (!strengths?.length) return null;
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

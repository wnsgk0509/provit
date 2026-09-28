import { useRef, useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowRight, FolderOpen, Layers, Sparkles } from 'lucide-react';
import { DOCUMENT_REVIEW_AVAILABLE, requestDocumentReview } from '../../api/documentReviewApi';
import { DOCUMENT_REVIEW_TYPES, REVIEW_FOCUS_OPTIONS, reviewErrorMessage } from './documentReviewConfig';
import { useReviewSelection } from './useReviewDocuments';
import ReviewDocumentWizard from './components/ReviewDocumentWizard';
import DocumentReviewResult from './components/DocumentReviewResult';
import './DocumentReview.css';

function DocumentReview() {
    const [searchParams, setSearchParams] = useSearchParams();
    // Old document links can still preselect one input in the new bundle.
    const legacyType = DOCUMENT_REVIEW_TYPES.find((type) => type.value === searchParams.get('type'));
    const selectedIds = Object.fromEntries(
        DOCUMENT_REVIEW_TYPES.map((type) => [
            type.selectionKey,
            searchParams.get(type.selectionKey) ||
                (legacyType?.value === type.value ? searchParams.get('document') : '') ||
                '',
        ]),
    );
    const resume = useReviewSelection('resume', selectedIds.resumeNum);
    const coverLetter = useReviewSelection('cover-letter', selectedIds.letterNum);
    const portfolio = useReviewSelection('portfolio', selectedIds.portfolioNum);
    const selections = { resumeNum: resume, letterNum: coverLetter, portfolioNum: portfolio };
    const [focusAreas, setFocusAreas] = useState(['expression', 'structure', 'evidence']);
    const [instructions, setInstructions] = useState('');
    const [submission, setSubmission] = useState(null);
    const [confirmedSelectionKey, setConfirmedSelectionKey] = useState(null);
    const submittingRef = useRef(false);
    const activeRef = useRef(false);
    const selectionKey = JSON.stringify(selectedIds);
    const contextKey = JSON.stringify({ selectedIds, focusAreas, instructions });
    const currentContextRef = useRef(contextKey);
    const currentSubmission = submission?.key === contextKey ? submission : null;
    const isSubmitting = currentSubmission?.status === 'loading';
    const requiredCount = Number(Boolean(resume.source.document)) + Number(Boolean(coverLetter.source.document));
    const portfolioReady = !selectedIds.portfolioNum || Boolean(portfolio.source.document);
    const isSelectionComplete = confirmedSelectionKey === selectionKey && requiredCount === 2 && portfolioReady;
    const canReview = isSelectionComplete && focusAreas.length > 0;

    useEffect(() => {
        activeRef.current = true;
        return () => {
            activeRef.current = false;
        };
    }, []);
    useEffect(() => {
        currentContextRef.current = contextKey;
    }, [contextKey]);

    const selectDocument = (key, value) => {
        setConfirmedSelectionKey(null);
        const next = { ...selectedIds, [key]: value };
        setSearchParams(Object.fromEntries(Object.entries(next).filter(([, id]) => id)), { replace: true });
    };
    const completeSelection = (skipPortfolio) => {
        const next = skipPortfolio ? { ...selectedIds, portfolioNum: '' } : selectedIds;
        if (skipPortfolio) {
            setSearchParams(Object.fromEntries(Object.entries(next).filter(([, id]) => id)), { replace: true });
        }
        setConfirmedSelectionKey(JSON.stringify(next));
    };
    const toggleFocus = (value) =>
        setFocusAreas((current) =>
            current.includes(value) ? current.filter((item) => item !== value) : [...current, value],
        );

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!DOCUMENT_REVIEW_AVAILABLE || submittingRef.current || !canReview) return;
        submittingRef.current = true;
        setSubmission({ key: contextKey, status: 'loading' });
        try {
            const result = await requestDocumentReview({
                resumeNum: Number(resume.selectedDocumentNum),
                letterNum: Number(coverLetter.selectedDocumentNum),
                portfolioNum: portfolio.selectedDocumentNum ? Number(portfolio.selectedDocumentNum) : null,
                focusAreas,
                instructions: instructions.trim(),
            });
            if (activeRef.current && currentContextRef.current === contextKey)
                setSubmission({ key: contextKey, status: 'complete', result });
        } catch (error) {
            if (activeRef.current && currentContextRef.current === contextKey)
                setSubmission({
                    key: contextKey,
                    status: 'error',
                    error: reviewErrorMessage(error, '통합 첨삭을 완료하지 못했습니다. 다시 시도해 주세요.'),
                });
        } finally {
            submittingRef.current = false;
        }
    };

    return (
        <div className="document-review-page">
            <header className="review-page-header">
                <div>
                    <span>AI ONE-CLICK REVIEW</span>
                    <h1>
                        AI원클릭첨삭 <Sparkles size={27} aria-hidden="true" />
                    </h1>
                    <p>이력서와 자기소개서, 포트폴리오를 함께 살펴보고 서류 간 일관성까지 확인하세요.</p>
                </div>
                <Link to="/mypage">
                    <FolderOpen size={16} aria-hidden="true" /> 취업 문서 관리{' '}
                    <ArrowRight size={16} aria-hidden="true" />
                </Link>
            </header>
            {!DOCUMENT_REVIEW_AVAILABLE && (
                <div className="review-availability" role="status">
                    <span>서비스 준비 중</span>
                    <p>첨삭할 서류를 함께 선택하고 통합 첨삭 결과 예시를 확인해 보세요.</p>
                </div>
            )}
            <div className="review-bundle-guide">
                <Layers size={22} aria-hidden="true" />
                <div>
                    <strong>서류를 모아, 한 번에 첨삭</strong>
                    <p>이력서 1개와 자기소개서 1개는 필수입니다. 포트폴리오 PDF는 선택적으로 추가할 수 있습니다.</p>
                </div>
            </div>
            <div className="review-workspace">
                <form className="review-setup" onSubmit={handleSubmit}>
                    <ReviewDocumentWizard
                        selections={selections}
                        isComplete={isSelectionComplete}
                        onChange={selectDocument}
                        onComplete={completeSelection}
                        onEdit={() => setConfirmedSelectionKey(null)}
                        disabled={isSubmitting}
                    />
                    {isSelectionComplete && (
                        <section className="review-card" aria-labelledby="review-options-title">
                            <div className="review-card-heading">
                                <span className="review-step">02</span>
                                <div>
                                    <h2 id="review-options-title">통합 첨삭 기준 설정</h2>
                                    <p>선택한 모든 서류에 같은 기준과 추가 요청을 적용합니다.</p>
                                </div>
                            </div>
                            <div className="review-consistency-guide">
                                <Layers size={16} aria-hidden="true" />
                                <span>서류 간 경력·역할·성과의 일관성은 기본으로 확인합니다.</span>
                            </div>
                            <fieldset className="review-focus-options" disabled={isSubmitting}>
                                <legend className="visually-hidden">첨삭 기준 · 하나 이상 선택</legend>
                                {REVIEW_FOCUS_OPTIONS.map((option) => (
                                    <label
                                        key={option.value}
                                        className={focusAreas.includes(option.value) ? 'is-selected' : ''}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={focusAreas.includes(option.value)}
                                            onChange={() => toggleFocus(option.value)}
                                        />
                                        <span>
                                            <strong>{option.label}</strong>
                                            <small>{option.description}</small>
                                        </span>
                                    </label>
                                ))}
                            </fieldset>
                            {!focusAreas.length && (
                                <p className="review-field-error" role="alert">
                                    첨삭 기준을 하나 이상 선택해 주세요.
                                </p>
                            )}
                            <div className="review-instructions">
                                <label htmlFor="review-instructions">
                                    추가 요청 <span>선택</span>
                                </label>
                                <textarea
                                    id="review-instructions"
                                    value={instructions}
                                    onChange={(event) => setInstructions(event.target.value)}
                                    maxLength={1000}
                                    rows={3}
                                    placeholder="예: 백엔드 개발자 지원용으로, 각 서류에 적은 프로젝트 역할과 성과가 일관되는지 봐 주세요."
                                    disabled={isSubmitting}
                                />
                                <span className="review-character-count">
                                    {instructions.length.toLocaleString()} / 1,000
                                </span>
                            </div>
                            <button
                                type="submit"
                                className="review-primary-button"
                                disabled={!DOCUMENT_REVIEW_AVAILABLE || !canReview || isSubmitting}
                                aria-describedby="review-submit-hint"
                            >
                                <Sparkles size={18} aria-hidden="true" />
                                {isSubmitting
                                    ? '통합 첨삭 진행 중...'
                                    : DOCUMENT_REVIEW_AVAILABLE
                                      ? '원클릭 통합 첨삭 시작하기'
                                      : 'AI원클릭첨삭 준비 중'}
                            </button>
                            <p id="review-submit-hint" className="review-submit-hint">
                                {requiredCount < 2
                                    ? '이력서와 자기소개서를 모두 선택해 주세요.'
                                    : !portfolioReady
                                      ? '포트폴리오를 확인할 수 없습니다. 다시 선택하거나 선택을 해제해 주세요.'
                                      : !DOCUMENT_REVIEW_AVAILABLE
                                        ? '필수 서류 선택이 완료되었습니다. 첨삭 서비스가 열리면 함께 요청할 수 있습니다.'
                                        : '선택한 서류를 한 번에 첨삭합니다. 수정 제안은 원문에 자동으로 반영되지 않습니다.'}
                            </p>
                        </section>
                    )}
                </form>
                <DocumentReviewResult
                    key={selectionKey}
                    includePortfolio={Boolean(portfolio.source.document)}
                    result={currentSubmission?.result}
                    isSubmitting={isSubmitting}
                    error={currentSubmission?.error}
                />
            </div>
        </div>
    );
}

export default DocumentReview;

import { useRef, useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, FolderOpen, Layers } from 'lucide-react';
import { requestDocumentReview, getDocumentReviewByRequestId } from '../../api/documentReviewApi';
import { saveReviewRequest, removeReviewRequest, resumeReviewRequest, reviewSelectionParams, createReviewRequestId } from './reviewRequest';
import {
    DOCUMENT_REVIEW_TYPES,
    REVIEW_CUSTOM_MAX_LENGTH,
    REVIEW_INSTRUCTIONS_MAX_LENGTH,
    REVIEW_MODE_OPTIONS,
    reviewErrorMessage,
} from './documentReviewConfig';
import { useReviewSelection, useSavedReview } from './useReviewDocuments';
import ReviewDocumentWizard from './components/ReviewDocumentWizard';
import ReviewOptions from './components/ReviewOptions';
import DocumentReviewResult from './components/DocumentReviewResult';
import ReviewHistory from './components/ReviewHistory';
import './DocumentReview.css';

const REVIEW_STEPS = ['서류 선택', '첨삭 기준 설정', '결과 확인'];

function DocumentReview() {
    const [searchParams, setSearchParams] = useSearchParams();
    const savedReviewNum = searchParams.get('reviewNum') || '';
    const pendingRequestId = searchParams.get('requestId') || '';
    const hasSavedRequest = Boolean(savedReviewNum || pendingRequestId);
    const [historyRevision, setHistoryRevision] = useState(0);
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
    const [reviewMode, setReviewMode] = useState('comprehensive');
    const [customCriteria, setCustomCriteria] = useState('');
    const [instructions, setInstructions] = useState('');
    const [submission, setSubmission] = useState(null);
    const savedReview = useSavedReview(savedReviewNum, submission?.result, pendingRequestId);
    const [confirmedSelectionKey, setConfirmedSelectionKey] = useState(null);
    const [step, setStep] = useState(() => pendingRequestId ? 2 : 0);
    const [exampleKey, setExampleKey] = useState(null);
    const workspaceRef = useRef(null);
    const submittingRef = useRef(false);
    const activeRef = useRef(false);
    const selectionKey = JSON.stringify(selectedIds);
    const effectiveCustomCriteria = reviewMode === 'custom' ? customCriteria.trim() : null;
    const contextKey = JSON.stringify({ selectedIds, reviewMode, customCriteria: effectiveCustomCriteria, instructions });
    const currentContextRef = useRef(contextKey);
    const currentSubmission = submission?.key === contextKey ? submission : null;
    const isSubmitting = currentSubmission?.status === 'loading' || savedReview.result?.reviewStatus === 'PROCESSING';
    const hasUnresolvedRequest = Boolean(pendingRequestId) && !['COMPLETED', 'FAILED'].includes(savedReview.result?.reviewStatus);
    const requiredCount = Number(Boolean(resume.source.document)) + Number(Boolean(coverLetter.source.document));
    const portfolioReady = !selectedIds.portfolioNum || Boolean(portfolio.source.document);
    const isSelectionComplete = confirmedSelectionKey === selectionKey && requiredCount === 2 && portfolioReady;
    const isCriteriaValid = REVIEW_MODE_OPTIONS.some((option) => option.value === reviewMode) &&
        (reviewMode !== 'custom' || (Boolean(effectiveCustomCriteria) && customCriteria.length <= REVIEW_CUSTOM_MAX_LENGTH));
    const canReview = isSelectionComplete && isCriteriaValid && instructions.length <= REVIEW_INSTRUCTIONS_MAX_LENGTH;
    const activeStep = savedReviewNum ? 2 : step === 1 && !isSelectionComplete ? 0 : step;
    const showExample = !hasSavedRequest && exampleKey === contextKey;
    const previousStepRef = useRef(activeStep);

    useEffect(() => {
        activeRef.current = true;
        return () => {
            activeRef.current = false;
        };
    }, []);
    useEffect(() => {
        currentContextRef.current = contextKey;
    }, [contextKey]);
    useEffect(() => {
        if (previousStepRef.current !== activeStep) {
            workspaceRef.current?.querySelector(`[data-review-step="${activeStep}"] h2`)?.focus();
        }
        previousStepRef.current = activeStep;
    }, [activeStep]);

    const selectDocument = (key, value) => {
        setConfirmedSelectionKey(null);
        const next = { ...selectedIds, [key]: value };
        setSearchParams((params) => reviewSelectionParams(params, next), { replace: true });
    };
    const completeSelection = (skipPortfolio) => {
        const next = skipPortfolio ? { ...selectedIds, portfolioNum: '' } : selectedIds;
        if (skipPortfolio) {
            setSearchParams((params) => reviewSelectionParams(params, next), { replace: true });
        }
        setConfirmedSelectionKey(JSON.stringify(next));
    };
    const changeCustomCriteria = (value) => setCustomCriteria(value.slice(0, REVIEW_CUSTOM_MAX_LENGTH));
    const changeInstructions = (value) => setInstructions(value.slice(0, REVIEW_INSTRUCTIONS_MAX_LENGTH));
    const clearSavedReview = () => {
        if (pendingRequestId && !hasUnresolvedRequest) removeReviewRequest(pendingRequestId, window.sessionStorage);
        setSearchParams((params) => {
            const next = new URLSearchParams(params);
            next.delete('reviewNum');
            if (!hasUnresolvedRequest) next.delete('requestId');
            return next;
        }, { replace: true });
    };
    const previewResult = () => {
        if (pendingRequestId) return;
        clearSavedReview();
        setExampleKey(contextKey);
        setStep(2);
    };
    const returnToSetup = () => {
        clearSavedReview();
        setStep(isSelectionComplete ? 1 : 0);
    };
    const openSavedReview = (reviewNum) => {
        setSearchParams((params) => {
            const next = new URLSearchParams(params);
            next.set('reviewNum', String(reviewNum));
            next.delete('requestId');
            return next;
        });
        setStep(2);
        workspaceRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (submittingRef.current || isSubmitting || !canReview) return;
        submittingRef.current = true;
        setExampleKey(null);
        setSubmission({ key: contextKey, status: 'loading' });
        setStep(2);
        try {
            const request = saveReviewRequest({
                resumeNum: Number(resume.selectedDocumentNum),
                letterNum: Number(coverLetter.selectedDocumentNum),
                portfolioNum: portfolio.selectedDocumentNum ? Number(portfolio.selectedDocumentNum) : null,
                reviewMode,
                customCriteria: effectiveCustomCriteria,
                instructions: instructions.trim(),
            }, pendingRequestId, window.sessionStorage, createReviewRequestId);
            setSearchParams((params) => {
                const next = new URLSearchParams(params);
                next.delete('reviewNum');
                next.set('requestId', request.requestId);
                return next;
            }, { replace: true });
            const result = await requestDocumentReview(request);
            if (activeRef.current) setHistoryRevision((value) => value + 1);
            if (activeRef.current && currentContextRef.current === contextKey) {
                setSubmission({ key: contextKey, status: 'complete', result });
                removeReviewRequest(request.requestId, window.sessionStorage);
                openSavedReview(result.reviewNum);
            }
        } catch (error) {
            if (activeRef.current) {
                setHistoryRevision((value) => value + 1);
                savedReview.reload();
            }
            if (activeRef.current && currentContextRef.current === contextKey)
                setSubmission({
                    key: contextKey,
                    status: 'error',
                    error: reviewErrorMessage(error, error.message || '통합 첨삭을 완료하지 못했습니다. 기존 요청 상태를 확인해 주세요.'),
                });
        } finally {
            submittingRef.current = false;
        }
    };

    const handleResume = async () => {
        if (submittingRef.current) return;
        submittingRef.current = true;
        setSubmission({ key: contextKey, status: 'loading' });
        try {
            const result = await resumeReviewRequest(pendingRequestId, window.sessionStorage,
                getDocumentReviewByRequestId, requestDocumentReview);
            if (!activeRef.current) return;
            setSubmission({ key: contextKey, status: 'complete', result });
            setHistoryRevision((value) => value + 1);
            removeReviewRequest(pendingRequestId, window.sessionStorage);
            openSavedReview(result.reviewNum);
        } catch (error) {
            if (activeRef.current) {
                savedReview.reload();
                setSubmission({ key: contextKey, status: 'error', error: reviewErrorMessage(error, error.message) });
            }
        } finally {
            submittingRef.current = false;
        }
    };

    return (
        <div className="document-review-page">
            <header className="review-page-header">
                <div>
                    <span>AI ONE-CLICK REVIEW</span>
                    <h1>AI원클릭첨삭</h1>
                    <p>서류 간 일관성과 개선사항을 확인하고, 지원 직무에 맞는 추천 스펙업도 확인해보세요.</p>
                </div>
                <Link to="/mypage">
                    <FolderOpen size={16} aria-hidden="true" /> 취업 문서 관리{' '}
                    <ArrowRight size={16} aria-hidden="true" />
                </Link>
            </header>
            <div className="review-availability" role="status">
                <span>AI 통합 첨삭</span>
                <p>선택한 서류를 한 번에 분석하고, 개선사항과 지원 직무에 도움이 될 준비 항목을 함께 저장합니다.</p>
            </div>
            <div className="review-bundle-guide">
                <Layers size={22} aria-hidden="true" />
                <div>
                    <strong>서류를 모아, 한 번에 첨삭</strong>
                    <p>이력서 1개와 자기소개서 1개는 필수입니다. 포트폴리오 PDF는 선택적으로 추가할 수 있습니다.</p>
                </div>
            </div>
            <div className="review-workspace">
                <nav className="review-progress" aria-label="통합 첨삭 진행 단계">
                    <p className="review-progress-title">첨삭 진행 순서</p>
                    <ol>
                        {REVIEW_STEPS.map((label, index) => {
                            const isDone = (index === 0 && isSelectionComplete) ||
                                (index === 1 && Boolean(currentSubmission));
                            return (
                                <li
                                    key={label}
                                    className={index === activeStep ? 'is-current' : isDone ? 'is-done' : ''}
                                >
                                    <button
                                        type="button"
                                        aria-current={index === activeStep ? 'step' : undefined}
                                        disabled={isSubmitting || (index === 1 && !isSelectionComplete) ||
                                            (index === 2 && !currentSubmission && !showExample && !hasSavedRequest)}
                                        onClick={() => {
                                            if (index !== 2) clearSavedReview();
                                            setStep(index);
                                        }}
                                    >
                                        <span className="review-progress-number">
                                            {isDone && index !== activeStep
                                                ? <Check size={15} aria-hidden="true" /> : index + 1}
                                        </span>
                                        <span>{label}</span>
                                    </button>
                                </li>
                            );
                        })}
                    </ol>
                    {activeStep !== 2 && !pendingRequestId && (
                        <button type="button" className="review-text-button" onClick={previewResult}>
                            결과 예시 보기 <ArrowRight size={14} aria-hidden="true" />
                        </button>
                    )}
                </nav>
                <div className="review-process" ref={workspaceRef}>
                    <form className="review-setup" onSubmit={handleSubmit} hidden={activeStep === 2}>
                        <div className="review-stage-container" data-review-step="0" hidden={activeStep !== 0}>
                            <ReviewDocumentWizard
                                selections={selections}
                                isComplete={isSelectionComplete}
                                onChange={selectDocument}
                                onComplete={completeSelection}
                                onEdit={() => setConfirmedSelectionKey(null)}
                                onContinue={() => setStep(1)}
                                disabled={isSubmitting}
                            />
                        </div>
                        {activeStep === 1 && (
                            <ReviewOptions
                                reviewMode={reviewMode}
                                customCriteria={customCriteria}
                                instructions={instructions}
                                canReview={canReview}
                                isSubmitting={isSubmitting}
                                onModeChange={setReviewMode}
                                onCustomCriteriaChange={changeCustomCriteria}
                                onInstructionsChange={changeInstructions}
                                onPrevious={() => setStep(0)}
                                onPreview={previewResult}
                            />
                        )}
                    </form>
                    {activeStep === 2 && (
                        <>
                            <DocumentReviewResult
                                includePortfolio={Boolean(portfolio.source.document)}
                                result={hasSavedRequest ? savedReview.result : currentSubmission?.result}
                                isSubmitting={isSubmitting || savedReview.isLoading}
                                error={hasSavedRequest ? savedReview.result ? savedReview.error : currentSubmission?.error || savedReview.error : currentSubmission?.error}
                                showExample={showExample}
                                onShowExample={previewResult}
                                onCloseExample={() => {
                                    setExampleKey(null);
                                    returnToSetup();
                                }}
                            />
                            {hasUnresolvedRequest && !isSubmitting && (
                                <button type="button" className="review-secondary-button" onClick={handleResume}>
                                    기존 요청 확인 · 이어서 진행
                                </button>
                            )}
                            {savedReviewNum && savedReview.error && (
                                <button type="button" className="review-secondary-button" onClick={savedReview.reload}>
                                    저장된 결과 다시 불러오기
                                </button>
                            )}
                            <div className="review-stage-actions">
                                <button
                                    type="button"
                                    className="review-secondary-button"
                                    onClick={returnToSetup}
                                    disabled={isSubmitting}
                                >
                                    <ArrowLeft size={16} aria-hidden="true" />
                                    {savedReview.result?.reviewStatus === 'FAILED' ? '새 첨삭 준비하기' : isSelectionComplete ? '첨삭 기준으로' : '서류 선택으로'}
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>
            <ReviewHistory key={historyRevision} currentReviewNum={savedReviewNum || String(savedReview.result?.reviewNum || '')} onOpen={openSavedReview} disabled={isSubmitting || hasUnresolvedRequest} />
        </div>
    );
}

export default DocumentReview;
